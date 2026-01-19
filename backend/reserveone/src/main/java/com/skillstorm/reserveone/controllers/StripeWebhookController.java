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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    // =========================
    // FIX: Robust deserialization
    // =========================

    private PaymentIntent getPaymentIntent(Event event) {
        try {
            // Attempt Stripe SDK typed deserialization first
            Object obj = event.getDataObjectDeserializer().getObject().orElse(null);
            if (obj instanceof PaymentIntent pi) {
                return pi;
            }

            // Fallback: parse raw JSON
            JsonObject dataObj = extractEventDataObject(event);
            if (dataObj == null) {
                return null;
            }

            // Stripe model classes include a configured GSON instance
            return PaymentIntent.GSON.fromJson(dataObj, PaymentIntent.class);
        } catch (Exception e) {
            log.warn("Failed to parse PaymentIntent from event {}: {}", safeEventId(event), e.getMessage());
            return null;
        }
    }

    private Charge getCharge(Event event) {
        try {
            Object obj = event.getDataObjectDeserializer().getObject().orElse(null);
            if (obj instanceof Charge ch) {
                return ch;
            }

            JsonObject dataObj = extractEventDataObject(event);
            if (dataObj == null) {
                return null;
            }

            return Charge.GSON.fromJson(dataObj, Charge.class);
        } catch (Exception e) {
            log.warn("Failed to parse Charge from event {}: {}", safeEventId(event), e.getMessage());
            return null;
        }
    }

    private JsonObject extractEventDataObject(Event event) {
        try {
            // event.toJson() gives the complete event JSON as received/parsed
            JsonObject root = JsonParser.parseString(event.toJson()).getAsJsonObject();
            if (root == null) {
                return null;
            }

            JsonObject data = root.getAsJsonObject("data");
            if (data == null) {
                return null;
            }

            JsonObject obj = data.getAsJsonObject("object");
            return obj;
        } catch (Exception e) {
            log.warn("Failed to extract event.data.object for event {}: {}", safeEventId(event), e.getMessage());
            return null;
        }
    }

    private String safeEventId(Event event) {
        try {
            return event == null ? "unknown" : String.valueOf(event.getId());
        } catch (Exception e) {
            return "unknown";
        }
    }

    // =========================
    // Handlers
    // =========================

    private String handlePaymentIntentSucceeded(Event event) {
        PaymentIntent pi = getPaymentIntent(event);
        if (pi == null) {
            // This was your current failure. The new getPaymentIntent should prevent it.
            return "ignored:missing_payment_intent";
        }

        // Prefer exact PI lookup first (most deterministic)
        String piId = pi.getId();
        if (StringUtils.hasText(piId)) {
            Optional<PaymentTransaction> byPi = findTxByPaymentIntentId(piId);
            if (byPi.isPresent()) {
                return markSucceeded(byPi.get(), piId);
            }
        }

        // Metadata fallback
        String reservationIdStr = getMetadata(pi, "reservation_id");
        String userIdStr = getMetadata(pi, "user_id");

        if (!StringUtils.hasText(reservationIdStr) || !StringUtils.hasText(userIdStr)) {
            return "tx_not_found";
        }

        UUID reservationId;
        UUID userId;

        try {
            reservationId = UUID.fromString(reservationIdStr.trim());
            userId = UUID.fromString(userIdStr.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid metadata UUID(s): reservation_id={}, user_id={}, pi={}",
                    reservationIdStr, userIdStr, piId);
            return "ignored:invalid_metadata_uuid";
        }

        Optional<PaymentTransaction> txOpt = paymentTransactionRepository
                .findByReservationIdAndUserId(reservationId, userId);

        if (txOpt.isEmpty()) {
            log.warn("No payment transaction found for reservation_id={}, user_id={}, pi={}",
                    reservationId, userId, piId);
            return "tx_not_found";
        }

        return markSucceeded(txOpt.get(), piId);
    }

    private String handlePaymentIntentFailed(Event event) {
        PaymentIntent pi = getPaymentIntent(event);
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
        Charge charge = getCharge(event);
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

    // =========================
    // Shared helpers
    // =========================

    private String markSucceeded(PaymentTransaction tx, String piId) {
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

        return "succeeded";
    }

    private String getMetadata(PaymentIntent pi, String key) {
        return (pi.getMetadata() == null) ? null : pi.getMetadata().get(key);
    }

    private String extractFailureMessage(PaymentIntent pi) {
        return (pi.getLastPaymentError() != null && pi.getLastPaymentError().getMessage() != null)
                ? pi.getLastPaymentError().getMessage()
                : "Unknown";
    }

    private Optional<PaymentTransaction> findTxByPaymentIntentId(String piId) {
        if (!StringUtils.hasText(piId)) {
            return Optional.empty();
        }

        // Primary: stripe_payment_intent_id column
        Optional<PaymentTransaction> byStripePi = paymentTransactionRepository.findByStripePaymentIntentId(piId);
        if (byStripePi.isPresent()) {
            return byStripePi;
        }

        // Secondary: transaction_id column (requires repository method below)
        try {
            return paymentTransactionRepository.findByTransactionId(piId);
        } catch (Exception e) {
            // If you haven't added findByTransactionId yet, you will hit a compile error, not runtime.
            return Optional.empty();
        }
    }
}
