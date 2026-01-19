package com.skillstorm.reserveone.controllers;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.PaymentTransactionRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationRepository reservationRepository;

    // application.properties:
    // stripe.webhook-secret=${STRIPE_WEBHOOK_SECRET:}
    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    public StripeWebhookController(
            PaymentTransactionRepository paymentTransactionRepository,
            ReservationRepository reservationRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> handleStripeWebhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader,
            @RequestBody String payload,
            HttpServletResponse res) {

        res.setHeader("X-Webhook-Reached", "YES");

        // If misconfigured, acknowledge to avoid Stripe retry storms, but log loudly
        if (!StringUtils.hasText(webhookSecret)) {
            log.error("Stripe webhook secret not configured (stripe.webhook-secret is blank)");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "webhook_secret_missing"));
        }

        // If signature header missing (CloudFront header forwarding), acknowledge but
        // log
        if (!StringUtils.hasText(sigHeader)) {
            log.warn("Missing Stripe-Signature header (check CloudFront forwarding)");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing_signature_header"));
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // Invalid signature can be malicious or misconfigured secret; acknowledge but
            // log
            log.warn("Stripe signature verification failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "invalid_signature"));
        } catch (Exception e) {
            log.warn("Stripe webhook parse/verify failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "invalid_payload"));
        }

        String result;
        try {
            result = switch (event.getType()) {
                case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
                case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
                case "charge.refunded" -> handleChargeRefunded(event);
                default -> "ignored:" + event.getType();
            };
        } catch (Exception e) {
            // For reliability: log error but ACK so Stripe doesn't retry forever
            log.error("Stripe webhook processing error: id={}, type={}",
                    event.getId(), event.getType(), e);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "processing_error"));
        }

        log.info("Stripe webhook handled: id={}, type={}, result={}",
                event.getId(), event.getType(), result);

        // Always return 200 for all webhook deliveries so Stripe does not retry
        return ResponseEntity.ok(Map.of("status", "ok", "result", result));
    }

    private String handlePaymentIntentSucceeded(Event event) {
        PaymentIntent pi = deserialize(event, PaymentIntent.class);
        if (pi == null) {
            return "ignored:missing_payment_intent";
        }

        String reservationIdStr = getMetadata(pi, "reservation_id");
        String userIdStr = getMetadata(pi, "user_id");

        // Preferred path: metadata present
        if (StringUtils.hasText(reservationIdStr) && StringUtils.hasText(userIdStr)) {
            UUID reservationId;
            UUID userId;

            try {
                reservationId = UUID.fromString(reservationIdStr);
                userId = UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid metadata UUID(s): reservation_id={}, user_id={}, pi={}",
                        reservationIdStr, userIdStr, pi.getId());
                return "ignored:invalid_metadata_uuid";
            }

            Optional<PaymentTransaction> txOpt = paymentTransactionRepository
                    .findByReservationIdAndUserId(reservationId, userId);

            if (txOpt.isEmpty()) {
                log.warn("No payment transaction found for reservation_id={}, user_id={}, pi={}",
                        reservationId, userId, pi.getId());
                return "ignored:tx_not_found";
            }

            PaymentTransaction tx = txOpt.get();
            if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED) {
                // Idempotent success
                return "already_succeeded";
            }

            tx.setStatus(PaymentTransaction.Status.SUCCEEDED);
            tx.setStripePaymentIntentId(pi.getId());
            paymentTransactionRepository.save(tx);

            reservationRepository.findById(reservationId).ifPresent(res -> {
                if (res.getStatus() != Reservation.Status.CONFIRMED) {
                    res.setStatus(Reservation.Status.CONFIRMED);
                    reservationRepository.save(res);
                }
            });

            return "succeeded";
        }

        // Fallback: lookup by payment intent ID
        return handleSucceededByPaymentIntentId(pi);
    }

    private String handleSucceededByPaymentIntentId(PaymentIntent pi) {
        String piId = pi.getId();
        if (!StringUtils.hasText(piId)) {
            return "ignored:missing_payment_intent_id";
        }

        return findTxByPaymentIntentId(piId)
                .map(tx -> {
                    if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED)
                        return "already_succeeded";

                    tx.setStatus(PaymentTransaction.Status.SUCCEEDED);

                    // (Optional but recommended) keep both columns aligned
                    tx.setStripePaymentIntentId(piId);
                    // If you have a setter for transactionId, set it too:
                    // tx.setTransactionId(piId);

                    paymentTransactionRepository.save(tx);

                    UUID reservationId = tx.getReservationId();
                    if (reservationId != null) {
                        reservationRepository.findById(reservationId).ifPresent(res -> {
                            if (res.getStatus() != Reservation.Status.CONFIRMED) {
                                res.setStatus(Reservation.Status.CONFIRMED);
                                reservationRepository.save(res);
                            }
                        });
                    }
                    return "succeeded_by_pi";
                })
                .orElse("tx_not_found");
    }

    private String handlePaymentIntentFailed(Event event) {
        PaymentIntent pi = deserialize(event, PaymentIntent.class);
        if (pi == null) {
            return "ignored:missing_payment_intent";
        }

        String piId = pi.getId();
        if (!StringUtils.hasText(piId)) {
            return "ignored:missing_payment_intent_id";
        }

        return findTxByPaymentIntentId(piId)
                .map(tx -> {
                    // Idempotency: if it already succeeded, do not downgrade it
                    if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED) {
                        return "already_succeeded";
                    }

                    tx.setStatus(PaymentTransaction.Status.FAILED);
                    tx.setStripePaymentIntentId(piId);
                    tx.setFailureReason(extractFailureMessage(pi));
                    paymentTransactionRepository.save(tx);

                    return "failed";
                })
                .orElse("tx_not_found");
    }

    private String handleChargeRefunded(Event event) {
        Charge charge = deserialize(event, Charge.class);
        if (charge == null) {
            return "ignored:missing_charge";
        }

        String paymentIntentId = charge.getPaymentIntent();
        if (!StringUtils.hasText(paymentIntentId)) {
            return "ignored:missing_payment_intent_id";
        }

        return findTxByPaymentIntentId(paymentIntentId)
                .map(tx -> {
                    tx.setStatus(PaymentTransaction.Status.REFUNDED);

                    // Optional: store Stripe charge id / receipt if your entity has fields for it
                    // tx.setStripeChargeId(charge.getId());
                    // tx.setReceiptUrl(charge.getReceiptUrl());

                    paymentTransactionRepository.save(tx);

                    // IMPORTANT:
                    // Your reservations.status constraint does NOT include REFUNDED.
                    // So do NOT set reservation status to REFUNDED unless you update DB + enum.
                    // A common approach is to CANCELLED the reservation on refund (if that matches
                    // your business rules).
                    UUID reservationId = tx.getReservationId();
                    if (reservationId != null) {
                        reservationRepository.findById(reservationId).ifPresent(res -> {
                            if (res.getStatus() != Reservation.Status.CANCELLED) {
                                res.setStatus(Reservation.Status.CANCELLED);
                                reservationRepository.save(res);
                            }
                        });
                    }

                    return "refunded";
                })
                .orElse("tx_not_found");
    }

    private String getMetadata(PaymentIntent pi, String key) {
        return (pi.getMetadata() == null) ? null : pi.getMetadata().get(key);
    }

    private String extractFailureMessage(PaymentIntent pi) {
        return (pi.getLastPaymentError() != null && pi.getLastPaymentError().getMessage() != null)
                ? pi.getLastPaymentError().getMessage()
                : "Unknown";
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(Event event, Class<T> clazz) {
        try {
            Object obj = event.getDataObjectDeserializer().getObject().orElse(null);
            if (obj == null || !clazz.isInstance(obj)) {
                return null;
            }
            return (T) obj;
        } catch (Exception e) {
            log.warn("Stripe event deserialization failed for {}: {}",
                    clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private Optional<PaymentTransaction> findTxByPaymentIntentId(String piId) {
        if (!StringUtils.hasText(piId)) {
            return Optional.empty();
        }
        return paymentTransactionRepository.findByStripePaymentIntentId(piId);
    }
}
