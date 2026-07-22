package com.bitcoinpaymentgateway.backend.dto;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Created Bitcoin payment request")
public record PaymentResponse(

        @Schema(
                description = "Unique payment identifier",
                example = "b1b827ea-7f47-4fca-a6ce-6753a4758c64"
        )
        UUID id,

        @Schema(
                description = "Payment amount expressed in satoshis",
                example = "50000"
        )
        Long amountSats,

        @Schema(
                description = "Simulated Bitcoin testnet address",
                example = "tb1q1234567890abcdef1234567890abcdef"
        )
        String bitcoinAddress,

        @Schema(
                description = "Current payment status",
                example = "PENDING"
        )
        PaymentStatus status,

        @Schema(description = "Payment creation timestamp")
        Instant createdAt,

        @Schema(description = "Payment expiration timestamp")
        Instant expiresAt,

        @Schema(description = "Timestamp when the payment was completed")
        Instant paidAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
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
