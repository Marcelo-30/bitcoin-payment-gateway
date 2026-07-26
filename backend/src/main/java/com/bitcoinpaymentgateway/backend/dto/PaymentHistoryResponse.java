package com.bitcoinpaymentgateway.backend.dto;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentHistoryResponse(
        UUID id,
        Long amountSats,
        String bitcoinAddress,
        PaymentStatus status,
        Instant createdAt,
        Instant expiresAt,
        Instant paidAt
) {
    public static PaymentHistoryResponse from(Payment payment) {
        return new PaymentHistoryResponse(
                payment.getId(),
                payment.getAmountSats(),
                payment.getBitcoinAddress(),
                payment.getStatus(),
                payment.getCreatedAt(),
                payment.getExpiresAt(),
                payment.getPaidAt()
        );
    }
}
