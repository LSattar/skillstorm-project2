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

/**
 * Service for managing payment transactions and integrating with Stripe payment processing.
 * 
 * <p>This service handles the payment workflow for reservations, including:
 * <ul>
 *   <li>Creating Stripe Payment Intents</li>
 *   <li>Tracking payment transaction status</li>
 *   <li>Confirming payment completion</li>
 * </ul>
 * 
 * <p>All payment operations are transactional and include validation to ensure
 * only authorized users can pay for their own reservations.
 * 
 * @author ReserveOne Team
 * @since 1.0
 */
@Service
public class PaymentService {

    private final ReservationRepository reservationRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final UserRepository userRepository;
    private final StripeService stripeService;

    /**
     * Constructs a new PaymentService with the required dependencies.
     * 
     * @param reservationRepository the repository for reservation data access
     * @param paymentTransactionRepository the repository for payment transaction data access
     * @param userRepository the repository for user data access
     * @param stripeService the service for Stripe API integration
     */
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

    /**
     * Creates a Stripe Payment Intent for a reservation and returns the client secret.
     * 
     * <p>This method performs the following operations:
     * <ol>
     *   <li><b>Authorization Check:</b> Verifies the user owns the reservation</li>
     *   <li><b>Status Validation:</b> Ensures reservation is in PENDING status (payable)</li>
     *   <li><b>Amount Conversion:</b> Converts reservation total amount to minor currency units
     *       (e.g., dollars to cents) for Stripe API</li>
     *   <li><b>Payment Intent Creation:</b> Creates a Stripe Payment Intent via StripeService</li>
     *   <li><b>Transaction Tracking:</b> Creates or updates a PaymentTransaction record
     *       with PROCESSING status</li>
     * </ol>
     * 
     * <p><b>Transaction Handling:</b> If a payment transaction already exists for this
     * reservation, it is reused and updated. Otherwise, a new transaction is created.
     * 
     * <p><b>Return Value:</b> The client secret from Stripe, which is used by the frontend
     * to complete the payment using Stripe's payment elements.
     * 
     * @param reservationId the UUID of the reservation to create payment for
     * @param userId the UUID of the user making the payment
     * @return the Stripe Payment Intent client secret for frontend payment completion
     * @throws ResourceNotFoundException if the reservation is not found
     * @throws ResourceConflictException if user doesn't own the reservation or reservation is not payable
     * @throws StripeException if there is an error communicating with Stripe API
     */
    @Transactional
    public String createPaymentIntent(UUID reservationId, UUID userId)
            throws StripeException {

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

        PaymentIntent pi = stripeService.createPaymentIntent(
                amountMinor,
                currency,
                reservationId.toString(),
                userId.toString(),
                "reservation-" + reservationId);

        Optional<PaymentTransaction> existing = paymentTransactionRepository.findByReservationId(reservationId);

        PaymentTransaction tx = existing.orElseGet(() -> new PaymentTransaction(
                UUID.randomUUID(),
                reservationId,
                userId,
                amount,
                currency,
                pi.getId()));

        tx.setStatus(PaymentTransaction.Status.PROCESSING);
        tx.setStripePaymentIntentId(pi.getId());

        paymentTransactionRepository.save(tx);
        return pi.getClientSecret();
    }

    /**
     * Confirms the status of a payment by querying Stripe and returns the transaction status.
     * 
     * <p>This method retrieves the current status of a Stripe Payment Intent and maps it
     * to the internal PaymentTransaction.Status enum:
     * <ul>
     *   <li><b>"succeeded"</b> → SUCCEEDED</li>
     *   <li><b>"processing"</b>, <b>"requires_capture"</b>, <b>"requires_confirmation"</b> → PROCESSING</li>
     *   <li><b>All other statuses</b> → FAILED</li>
     * </ul>
     * 
     * <p><b>Note:</b> This method only returns the status. The actual status update
     * should be handled by the Stripe webhook handler for real-time updates.
     * 
     * @param reservationId the UUID of the reservation associated with the payment
     * @param userId the UUID of the user who made the payment
     * @param paymentIntentId the Stripe Payment Intent ID to check
     * @return the mapped PaymentTransaction.Status based on Stripe's payment intent status
     * @throws ResourceNotFoundException if the payment transaction is not found
     * @throws StripeException if there is an error communicating with Stripe API
     */
    public PaymentTransaction.Status confirmPayment(
            UUID reservationId,
            UUID userId,
            String paymentIntentId) throws StripeException {

        PaymentTransaction tx = paymentTransactionRepository
                .findByReservationIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        PaymentIntent pi = stripeService.retrievePaymentIntent(paymentIntentId);

        return switch (pi.getStatus()) {
            case "succeeded" -> PaymentTransaction.Status.SUCCEEDED;
            case "processing", "requires_capture", "requires_confirmation" ->
                PaymentTransaction.Status.PROCESSING;
            default -> PaymentTransaction.Status.FAILED;
        };
    }
}
