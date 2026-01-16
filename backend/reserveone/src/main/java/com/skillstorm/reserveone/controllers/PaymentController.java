package com.skillstorm.reserveone.controllers;

import java.util.Map;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.UserRepository;
import com.skillstorm.reserveone.services.PaymentService;
import com.stripe.exception.StripeException;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;

    public PaymentController(
            PaymentService paymentService,
            ReservationRepository reservationRepository,
            UserRepository userRepository) {
        this.paymentService = paymentService;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/intents")
    public Map<String, String> createPaymentIntent(
            @RequestBody Map<String, String> body,
            Authentication authentication) throws StripeException {

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
    public Map<String, Object> confirmPayment(
            @RequestBody Map<String, String> body,
            Authentication authentication) throws StripeException {

        UUID reservationId = UUID.fromString(body.get("reservationId"));
        String paymentIntentId = body.get("paymentIntentId");
        UUID userId = getUserIdFromPrincipal(authentication);

        PaymentTransaction.Status status = paymentService.confirmPayment(reservationId, userId, paymentIntentId);

        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);

        return Map.of(
                "status", status.name(),
                "reservationStatus",
                reservation != null ? reservation.getStatus().name() : null);
    }

    private UUID getUserIdFromPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof com.skillstorm.reserveone.models.User user) {
            return user.getUserId();
        }

        if (principal instanceof OidcUser oidcUser) {
            return userRepository
                    .findByEmail(oidcUser.getEmail())
                    .orElseThrow(() -> new IllegalStateException("User not found"))
                    .getUserId();
        }

        if (principal instanceof OAuth2User oauth2User) {
            return userRepository
                    .findByEmail(oauth2User.getAttribute("email"))
                    .orElseThrow(() -> new IllegalStateException("User not found"))
                    .getUserId();
        }

        throw new IllegalStateException("Unsupported authentication principal");
    }
}
