package com.skillstorm.reserveone.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    // Optional but useful for debugging and tracing in Stripe metadata
    @Value("${app.name:reserveone}")
    private String appName;

    // Optional; set via env var or EB config if you want (prod, staging, dev, etc.)
    @Value("${app.env:prod}")
    private String appEnv;

    @PostConstruct
    public void validateConfig() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY is not configured.");
        }
    }

    private RequestOptions baseOptions() {
        return RequestOptions.builder()
                .setApiKey(secretKey)
                .build();
    }

    public PaymentIntent createPaymentIntent(
            long amount,
            String currency,
            String reservationId,
            String userId,
            String idempotencyKey) throws StripeException {

        String safeReservationId = reservationId == null ? "" : reservationId.trim();
        String safeUserId = userId == null ? "" : userId.trim();
        String safeCurrency = currency == null ? "usd" : currency.trim().toLowerCase();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(safeCurrency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("reservation_id", safeReservationId)
                .putMetadata("user_id", safeUserId)
                .putMetadata("app", appName == null ? "reserveone" : appName.trim())
                .putMetadata("env", appEnv == null ? "prod" : appEnv.trim())
                .build();

        RequestOptions options = baseOptions().toBuilder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        return PaymentIntent.create(params, options);
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId, baseOptions());
    }
}
