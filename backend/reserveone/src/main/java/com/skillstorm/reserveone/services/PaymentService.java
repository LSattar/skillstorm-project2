package com.skillstorm.reserveone.services;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.skillstorm.reserveone.exceptions.ResourceConflictException;
import com.skillstorm.reserveone.exceptions.ResourceNotFoundException;
import com.skillstorm.reserveone.models.PaymentTransaction;
import com.skillstorm.reserveone.models.Reservation;
import com.skillstorm.reserveone.repositories.PaymentTransactionRepository;
import com.skillstorm.reserveone.repositories.ReservationRepository;
import com.skillstorm.reserveone.repositories.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import jakarta.transaction.Transactional;

@Service
public class PaymentService {

    private final ReservationRepository reservationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    public PaymentService(
            ReservationRepository reservationRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            UserRepository userRepository,
            StripeService stripeService) {
        this.reservationRepository = reservationRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.userRepository = userRepository;
        this.stripeService = stripeService;
    }

    @Transactional
    public String createPaymentIntent(UUID reservationId, UUID userId) throws StripeException {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));

        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new ResourceConflictException("You do not own this reservation");
        }

        if (reservation.getStatus() != Reservation.Status.PENDING) {
            throw new ResourceConflictException("Reservation is not payable");
        }

        BigDecimal amount = reservation.getTotalAmount();
        String currency = reservation.getCurrency();
        long amountMinor = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

        String idempotencyKey = "reservation-" + reservationId;

        PaymentIntent paymentIntent = stripeService.createPaymentIntent(
                amountMinor,
                currency,
                reservationId.toString(),
                userId.toString(),
                idempotencyKey);

        // Upsert payment transaction
        Optional<PaymentTransaction> existing = paymentTransactionRepository.findByReservationId(reservationId);
        PaymentTransaction tx = existing.orElseGet(() -> new PaymentTransaction(UUID.randomUUID(), reservationId,
                userId, amount, currency, paymentIntent.getId()));
        tx.setStatus(PaymentTransaction.Status.PROCESSING);
        tx.setStripePaymentIntentId(paymentIntent.getId());
        tx.setAmount(amount);
        tx.setCurrency(currency);
        paymentTransactionRepository.save(tx);

        return paymentIntent.getClientSecret();
    }

    @Transactional
    public PaymentTransaction.Status confirmPayment(UUID reservationId, UUID userId, String paymentIntentId)
            throws StripeException {
        PaymentTransaction tx = paymentTransactionRepository.findByReservationIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found"));

        if (!tx.getStripePaymentIntentId().equals(paymentIntentId)) {
            throw new ResourceConflictException("PaymentIntent does not match reservation");
        }

        PaymentIntent pi = stripeService.retrievePaymentIntent(paymentIntentId);

        switch (pi.getStatus()) {
            case "succeeded":
                return PaymentTransaction.Status.SUCCEEDED;
            case "processing":
            case "requires_capture":
            case "requires_confirmation":
                return PaymentTransaction.Status.PROCESSING;
            case "requires_payment_method":
            case "canceled":
            case "requires_action":
            case "requires_source":
            case "requires_source_action":
            case "failed":
            default:
                return PaymentTransaction.Status.FAILED;
        }
    }
}
