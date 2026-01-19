package com.skillstorm.reserveone.controllers;

import java.util.Enumeration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.PaymentTransactionRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationRepository reservationRepository;

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
            // CloudFront Function will copy Stripe-Signature into this safe header:
            @RequestHeader(value = "X-Stripe-Signature", required = false) String xStripeSigHeader,
            @RequestBody String payload,
            HttpServletRequest req,
            HttpServletResponse res) {

        res.setHeader("X-Webhook-Reached", "YES");

        // If CloudFront stripped the original header, use the injected one
        if (!StringUtils.hasText(sigHeader) && StringUtils.hasText(xStripeSigHeader)) {
            sigHeader = xStripeSigHeader;
            log.info("Using X-Stripe-Signature fallback header for verification");
        }

        // ---- DEBUG: log what CloudFront/ALB actually sends ----
        try {
            log.info("WEBHOOK DEBUG start: method={}, uri={}, remoteAddr={}",
                    req.getMethod(), req.getRequestURI(), req.getRemoteAddr());

            log.info("WEBHOOK DEBUG x-forwarded-for={}", req.getHeader("X-Forwarded-For"));
            log.info("WEBHOOK DEBUG x-forwarded-proto={}", req.getHeader("X-Forwarded-Proto"));
            log.info("WEBHOOK DEBUG x-forwarded-host={}", req.getHeader("X-Forwarded-Host"));
            log.info("WEBHOOK DEBUG host={}", req.getHeader("Host"));
            log.info("WEBHOOK DEBUG content-type={}", req.getHeader("Content-Type"));

            Enumeration<String> names = req.getHeaderNames();
            while (names != null && names.hasMoreElements()) {
                String name = names.nextElement();
                String value = req.getHeader(name);
                log.info("WEBHOOK HDR {} = {}", name, value);
            }

            log.info("WEBHOOK DEBUG Spring-bound Stripe-Signature={}", sigHeader);
            log.info("WEBHOOK DEBUG Spring-bound X-Stripe-Signature={}", xStripeSigHeader);
            log.info("WEBHOOK DEBUG Servlet getHeader('Stripe-Signature')={}", req.getHeader("Stripe-Signature"));
            log.info("WEBHOOK DEBUG Servlet getHeader('stripe-signature')={}", req.getHeader("stripe-signature"));
            log.info("WEBHOOK DEBUG Servlet getHeader('X-Stripe-Signature')={}", req.getHeader("X-Stripe-Signature"));
            log.info("WEBHOOK DEBUG Servlet getHeader('x-stripe-signature')={}", req.getHeader("x-stripe-signature"));
            log.info("WEBHOOK DEBUG payloadLength={}", payload == null ? 0 : payload.length());
        } catch (Exception e) {
            log.warn("WEBHOOK DEBUG logging failed: {}", e.getMessage());
        }
        // ---- END DEBUG ----

        if (!StringUtils.hasText(webhookSecret)) {
            log.error("Stripe webhook secret not configured (stripe.webhook-secret is blank)");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "webhook_secret_missing"));
        }

        if (!StringUtils.hasText(sigHeader)) {
            log.warn("Missing Stripe-Signature header (check CloudFront forwarding and function injection)");
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "missing_signature_header"));
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
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
            log.error("Stripe webhook processing error: id={}, type={}",
                    event.getId(), event.getType(), e);
            return ResponseEntity.ok(Map.of("status", "ignored", "reason", "processing_error"));
        }

        log.info("Stripe webhook handled: id={}, type={}, result={}",
                event.getId(), event.getType(), result);

        return ResponseEntity.ok(Map.of("status", "ok", "result", result));
    }

    private String handlePaymentIntentSucceeded(Event event) {
        PaymentIntent pi = deserialize(event, PaymentIntent.class);
        if (pi == null) {
            return "ignored:missing_payment_intent";
        }

        String reservationIdStr = getMetadata(pi, "reservation_id");
        String userIdStr = getMetadata(pi, "user_id");

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

        return handleSucceededByPaymentIntentId(pi);
    }

    private String handleSucceededByPaymentIntentId(PaymentIntent pi) {
        String piId = pi.getId();
        if (!StringUtils.hasText(piId)) {
            return "ignored:missing_payment_intent_id";
        }

        return findTxByPaymentIntentId(piId)
                .map(tx -> {
                    if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED) {
                        return "already_succeeded";
                    }

                    tx.setStatus(PaymentTransaction.Status.SUCCEEDED);
                    tx.setStripePaymentIntentId(piId);

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
                    paymentTransactionRepository.save(tx);

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
