package com.skillstorm.reserveone.mappers;

import org.springframework.stereotype.Component;

import com.skillstorm.reserveone.dto.PaymentTransactionDto;
import com.skillstorm.reserveone.models.PaymentTransaction;

@Component
public class PaymentTransactionMapper {
    public PaymentTransactionDto toDto(PaymentTransaction entity) {
        if (entity == null)
            return null;
        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.setId(entity.getPaymentId());
        dto.setReservationId(entity.getReservationId());
        dto.setUserId(entity.getUserId());
        dto.setAmount(entity.getAmount());
        dto.setCurrency(entity.getCurrency());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setProvider(entity.getProvider());
        // Map paymentMethodLast4 if available (not in entity, so set null)
        dto.setPaymentMethodLast4(null); // Adjust if field exists
        // Map transactionId (try stripePaymentIntentId, stripeChargeId, or
        // transaction_id)
        dto.setTransactionId(entity.getStripePaymentIntentId() != null ? entity.getStripePaymentIntentId()
                : entity.getStripeChargeId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
