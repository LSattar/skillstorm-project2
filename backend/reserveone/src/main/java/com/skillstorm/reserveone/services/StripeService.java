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

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build())
                .putMetadata("reservation_id", reservationId)
                .putMetadata("user_id", userId)
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
