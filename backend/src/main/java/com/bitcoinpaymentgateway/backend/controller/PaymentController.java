package com.bitcoinpaymentgateway.backend.controller;

import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.CreatePaymentRequest;
import com.bitcoinpaymentgateway.backend.dto.PaymentHistoryResponse;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payments",
        description = "Operations for Bitcoin payment requests"
)
public class PaymentController {

        private final PaymentService paymentService;

        @PostMapping
        @Operation(
                summary = "Create a payment request",
                description = """
                    Creates a simulated Bitcoin payment request,
                    assigns a testnet address and sets the initial
                    status to PENDING.
                    """
        )
        @ApiResponses({
                @ApiResponse(
                        responseCode = "201",
                        description = "Payment request created successfully",
                        content = @Content(
                                schema = @Schema(
                                        implementation = PaymentResponse.class
                                )
                        )
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid payment amount",
                        content = @Content
                )
        })
        public ResponseEntity<PaymentResponse> createPayment(
                @Valid @RequestBody CreatePaymentRequest request
        ) {
                PaymentResponse response =
                        paymentService.createPayment(request);

                return ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(response);
        }

        @GetMapping("/{id}")
        @Operation(
                summary = "Get a payment by ID",
                description = """
                    Retrieves a payment and resolves its expiration
                    status if needed.
                    """
        )
        @ApiResponses({
                @ApiResponse(
                        responseCode = "200",
                        description = "Payment found"
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "Payment not found",
                        content = @Content
                )
        })
        public ResponseEntity<PaymentResponse> getPayment(
                @PathVariable UUID id
        ) {
                PaymentResponse response =
                        paymentService.getPayment(id);

                return ResponseEntity.ok(response);
        }

        @GetMapping
        @Operation(
                summary = "Get payment history",
                description = """
                    Returns payments using pagination. Results are sorted
                    from newest to oldest and may be filtered by status.
                    """
        )
        @ApiResponses({
                @ApiResponse(
                        responseCode = "200",
                        description = "Payment history returned successfully"
                ),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid pagination or status parameter",
                        content = @Content
                )
        })
        public ResponseEntity<Page<PaymentHistoryResponse>> getPayments(
                @Parameter(
                        description = "Requested page number, starting at zero",
                        example = "0"
                )
                @RequestParam(defaultValue = "0")
                @Min(
                        value = 0,
                        message = "page must be zero or greater"
                )
                int page,

                @Parameter(
                        description = "Number of payments per page",
                        example = "10"
                )
                @RequestParam(defaultValue = "10")
                @Min(
                        value = 1,
                        message = "size must be greater than zero"
                )
                @Max(
                        value = 100,
                        message = "size must not exceed 100"
                )
                int size,

                @Parameter(
                        description = """
                            Optional payment status filter.
                            Supported values: PENDING, PAID and EXPIRED.
                            """,
                        example = "PENDING"
                )
                @RequestParam(required = false)
                PaymentStatus status
        ) {
                Page<PaymentHistoryResponse> response =
                        paymentService.getPayments(page, size, status);

                return ResponseEntity.ok(response);
        }
}