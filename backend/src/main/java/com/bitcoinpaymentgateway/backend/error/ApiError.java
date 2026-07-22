package com.bitcoinpaymentgateway.backend.error;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;

/**
 * Standard error body returned by every endpoint when a request fails, so
 * clients can rely on a single, consistent shape.
 */
@Schema(description = "Standard error response returned for any failed request")
public record ApiError(

        @Schema(description = "When the error was produced", example = "2026-07-22T10:15:30Z")
        Instant timestamp,

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "HTTP status reason phrase", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable explanation of what went wrong",
                example = "amountSats must be greater than zero")
        String message,

        @Schema(description = "Request path that produced the error", example = "/api/payments")
        String path
) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path);
    }
}
