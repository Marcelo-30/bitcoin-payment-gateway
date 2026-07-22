package com.bitcoinpaymentgateway.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePaymentRequest(

        @NotNull(message = "amountSats is required")
        @Positive(message = "amountSats must be greater than zero")
        @Schema(
                description = "Payment amount expressed in satoshis",
                example = "50000",
                minimum = "1"
        )
        Long amountSats
) {
}
