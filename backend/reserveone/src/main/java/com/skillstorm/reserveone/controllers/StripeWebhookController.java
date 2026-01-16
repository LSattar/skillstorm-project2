package com.skillstorm.reserveone.controllers;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.PaymentTransactionRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.stripe.model.Charge;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;

import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final ReservationRepository reservationRepository;

    @Value("${stripe.webhook-secret}")
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
            @RequestHeader("Stripe-Signature") String sigHeader,
            @RequestBody String payload) {
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        final String eventId = event.getId();
        final String eventType = event.getType();

        try {
            final String result = switch (eventType) {
                case "payment_intent.succeeded" -> onPaymentIntentSucceeded(event);
                case "payment_intent.payment_failed" -> onPaymentIntentFailed(event);
                case "charge.refunded" -> onChargeRefunded(event);
                default -> "ignored";
            };

            log.info("Stripe webhook handled: id={}, type={}, result={}", eventId, eventType, result);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stripe webhook processing failed: id={}, type={}, err={}", eventId, eventType, e.getMessage());
            return ResponseEntity.status(500).body("Webhook processing error");
        }
    }

    private String onPaymentIntentSucceeded(Event event) {
        PaymentIntent pi = deserialize(event, PaymentIntent.class);
        if (pi == null)
            return "missing_payment_intent";

        String reservationIdStr = pi.getMetadata() != null ? pi.getMetadata().get("reservation_id") : null;
        String userIdStr = pi.getMetadata() != null ? pi.getMetadata().get("user_id") : null;
        if (reservationIdStr == null || userIdStr == null)
            return "missing_metadata";

        UUID reservationId = UUID.fromString(reservationIdStr);
        UUID userId = UUID.fromString(userIdStr);

        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByReservationIdAndUserId(reservationId,
                userId);

        if (txOpt.isEmpty())
            return "tx_not_found";

        PaymentTransaction tx = txOpt.get();
        if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED)
            return "already_succeeded";

        tx.setStatus(PaymentTransaction.Status.SUCCEEDED);
        tx.setStripeChargeId(getChargeId(pi));
        tx.setReceiptUrl(getReceiptUrl(pi));
        paymentTransactionRepository.save(tx);

        reservationRepository.findById(reservationId).ifPresent(res -> {
            if (res.getStatus() != Reservation.Status.CONFIRMED) {
                res.setStatus(Reservation.Status.CONFIRMED);
                reservationRepository.save(res);
            }
        });

        return "succeeded";
    }

    private String onPaymentIntentFailed(Event event) {
        PaymentIntent pi = deserialize(event, PaymentIntent.class);
        if (pi == null)
            return "missing_payment_intent";

        String reservationIdStr = pi.getMetadata() != null ? pi.getMetadata().get("reservation_id") : null;
        String userIdStr = pi.getMetadata() != null ? pi.getMetadata().get("user_id") : null;
        if (reservationIdStr == null || userIdStr == null)
            return "missing_metadata";

        UUID reservationId = UUID.fromString(reservationIdStr);
        UUID userId = UUID.fromString(userIdStr);

        Optional<PaymentTransaction> txOpt = paymentTransactionRepository.findByReservationIdAndUserId(reservationId,
                userId);

        if (txOpt.isEmpty())
            return "tx_not_found";

        PaymentTransaction tx = txOpt.get();
        if (tx.getStatus() == PaymentTransaction.Status.SUCCEEDED)
            return "already_succeeded";

        tx.setStatus(PaymentTransaction.Status.FAILED);
        String msg = (pi.getLastPaymentError() != null && pi.getLastPaymentError().getMessage() != null)
                ? pi.getLastPaymentError().getMessage()
                : "Unknown";
        tx.setFailureReason(msg);
        paymentTransactionRepository.save(tx);

        return "failed";
    }

    private String onChargeRefunded(Event event) {
        Charge charge = deserialize(event, Charge.class);
        if (charge == null)
            return "missing_charge";

        String paymentIntentId = charge.getPaymentIntent();
        if (paymentIntentId == null)
            return "missing_payment_intent";

        return paymentTransactionRepository.findByStripePaymentIntentId(paymentIntentId)
                .map(tx -> {
                    tx.setStatus(PaymentTransaction.Status.REFUNDED);
                    paymentTransactionRepository.save(tx);
                    return "refunded";
                })
                .orElse("tx_not_found");
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(Event event, Class<T> clazz) {
        try {
            Object obj = event.getDataObjectDeserializer().getObject().orElse(null);
            if (obj == null)
                return null;
            if (!clazz.isInstance(obj))
                return null;
            return (T) obj;
        } catch (Exception e) {
            log.warn("Stripe event deserialization failed for {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    private String getChargeId(PaymentIntent pi) {
        try {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("payment_intent", pi.getId());
            com.stripe.model.ChargeCollection charges = com.stripe.model.Charge.list(params);
            if (charges != null && charges.getData() != null && !charges.getData().isEmpty()) {
                return charges.getData().get(0).getId();
            }
        } catch (Exception e) {
            log.error("Failed to fetch charges for PaymentIntent {}: {}", pi.getId(), e.getMessage());
        }
        return null;
    }

    private String getReceiptUrl(PaymentIntent pi) {
        try {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("payment_intent", pi.getId());
            com.stripe.model.ChargeCollection charges = com.stripe.model.Charge.list(params);
            if (charges != null && charges.getData() != null && !charges.getData().isEmpty()) {
                return charges.getData().get(0).getReceiptUrl();
            }
        } catch (Exception e) {
            log.error("Failed to fetch charges for PaymentIntent {}: {}", pi.getId(), e.getMessage());
        }
        return null;
    }
}
