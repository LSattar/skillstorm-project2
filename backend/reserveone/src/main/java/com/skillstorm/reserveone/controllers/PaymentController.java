package com.skillstorm.reserveone.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.services.PaymentService;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationRepository reservationRepository;

    public PaymentController(PaymentService paymentService, ReservationRepository reservationRepository) {
        this.paymentService = paymentService;
        this.reservationRepository = reservationRepository;
    }

    @PostMapping("/intents")
    public Map<String, String> createPaymentIntent(@RequestBody Map<String, String> body, Authentication authentication)
            throws StripeException {
        String reservationIdStr = body.get("reservationId");
        if (reservationIdStr == null) {
            throw new IllegalArgumentException("reservationId is required");
        }
        UUID reservationId = UUID.fromString(reservationIdStr);
        UUID userId = getUserIdFromPrincipal(authentication);
        String clientSecret = paymentService.createPaymentIntent(reservationId, userId);
        return Map.of("clientSecret", clientSecret);
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirmPayment(@RequestBody Map<String, String> body, Authentication authentication)
            throws StripeException {
        String reservationIdStr = body.get("reservationId");
        String paymentIntentId = body.get("paymentIntentId");
        if (reservationIdStr == null || paymentIntentId == null) {
            throw new IllegalArgumentException("reservationId and paymentIntentId are required");
        }
        UUID reservationId = UUID.fromString(reservationIdStr);
        UUID userId = getUserIdFromPrincipal(authentication);

        PaymentTransaction.Status status = paymentService.confirmPayment(reservationId, userId, paymentIntentId);
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);

        return Map.of(
                "status", status.name(),
                "reservationStatus", reservation != null ? reservation.getStatus().name() : null);
    }

    private UUID getUserIdFromPrincipal(Authentication authentication) {
        // Adapt this to your principal structure
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.skillstorm.reserveone.models.User user) {
            return user.getUserId();
        }
        // If using JWT or custom principal, extract userId accordingly
        throw new IllegalStateException("Unable to extract userId from principal");
    }
}
