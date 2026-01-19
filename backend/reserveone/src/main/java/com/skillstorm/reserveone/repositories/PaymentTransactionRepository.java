package com.skillstorm.reserveone.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.skillstorm.reserveone.models.PaymentTransaction;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, UUID>, JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByStripePaymentIntentId(String paymentIntentId);

    Optional<PaymentTransaction> findByReservationId(UUID reservationId);

    Optional<PaymentTransaction> findByReservationIdAndUserId(UUID reservationId, UUID userId);
}
