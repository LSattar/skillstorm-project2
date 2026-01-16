package com.skillstorm.reserveone.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

import jakarta.annotation.PostConstruct;

@Service
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.api-version:2023-10-16}")
    private String apiVersion;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        Stripe.setAppInfo("ReserveOne", "1.0.0", null, null);
        // Stripe API version can be set in the dashboard or via RequestOptions if
        // needed
    }

    public PaymentIntent createPaymentIntent(long amount, String currency, String reservationId, String userId,
            String idempotencyKey) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .putMetadata("reservation_id", reservationId)
                .putMetadata("user_id", userId)
                .build();

        com.stripe.net.RequestOptions requestOptions = com.stripe.net.RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();
        return PaymentIntent.create(params, requestOptions);
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        return PaymentIntent.retrieve(paymentIntentId);
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public String verifyWebhookSignature(String payload, String sigHeader) throws Exception {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret).getId();
    }
}
