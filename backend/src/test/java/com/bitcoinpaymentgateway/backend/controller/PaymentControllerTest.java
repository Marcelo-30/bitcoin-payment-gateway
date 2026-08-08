package com.bitcoinpaymentgateway.backend.controller;

import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.error.ResourceNotFoundException;
import com.bitcoinpaymentgateway.backend.error.PaymentStateConflictException;
import com.bitcoinpaymentgateway.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.bitcoinpaymentgateway.backend.dto.PaymentHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.mockito.Mockito.verify;


import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PaymentService paymentService;

        @Test
        void shouldReturnCreatedWhenPaymentIsValid() throws Exception {
                UUID paymentId = UUID.randomUUID();
                Instant createdAt = Instant.parse("2026-07-21T17:00:00Z");
                Instant expiresAt = createdAt.plusSeconds(900);

                PaymentResponse response = new PaymentResponse(
                                paymentId,
                                50_000L,
                                "tb1q1234567890abcdef1234567890abcdef",
                                PaymentStatus.PENDING,
                                createdAt,
                                expiresAt,
                                null);

                when(paymentService.createPayment(any()))
                                .thenReturn(response);

                mockMvc.perform(
                                post("/api/payments")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "amountSats": 50000
                                                                }
                                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id")
                                                .value(paymentId.toString()))
                                .andExpect(jsonPath("$.amountSats")
                                                .value(50000))
                                .andExpect(jsonPath("$.status")
                                                .value("PENDING"))
                                .andExpect(jsonPath("$.bitcoinAddress")
                                                .value(
                                                                "tb1q1234567890abcdef1234567890abcdef"));
        }

        @Test
        void shouldReturnBadRequestWhenAmountIsZero()
                        throws Exception {

                mockMvc.perform(
                                post("/api/payments")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                  "amountSats": 0
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(paymentService);
        }

        @Test
        void shouldReturnBadRequestWhenAmountIsMissing()
                        throws Exception {

                mockMvc.perform(
                                post("/api/payments")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("""
                                                                {
                                                                }
                                                                """))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(paymentService);
        }

        @Test
        void shouldReturnPaymentWhenFound() throws Exception {
                UUID paymentId = UUID.randomUUID();
                Instant createdAt = Instant.parse("2026-07-21T17:00:00Z");
                Instant expiresAt = createdAt.plusSeconds(900);

                PaymentResponse response = new PaymentResponse(
                                paymentId,
                                50_000L,
                                "tb1q1234567890abcdef1234567890abcdef",
                                PaymentStatus.PENDING,
                                createdAt,
                                expiresAt,
                                null);

                when(paymentService.getPayment(paymentId))
                                .thenReturn(response);

                mockMvc.perform(get("/api/payments/{id}", paymentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id")
                                                .value(paymentId.toString()))
                                .andExpect(jsonPath("$.status")
                                                .value("PENDING"));
        }

        @Test
        void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
                UUID paymentId = UUID.randomUUID();

                when(paymentService.getPayment(paymentId))
                                .thenThrow(new ResourceNotFoundException(
                                                "Payment not found: " + paymentId));

                mockMvc.perform(get("/api/payments/{id}", paymentId))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message")
                                                .value("Payment not found: " + paymentId));
        }

        @Test
        void shouldSimulatePayment() throws Exception {
                UUID paymentId = UUID.randomUUID();
                Instant createdAt = Instant.parse("2026-07-21T17:00:00Z");
                Instant paidAt = createdAt.plusSeconds(60);
                PaymentResponse response = new PaymentResponse(
                                paymentId, 50_000L, "tb1qaddress",
                                PaymentStatus.PAID, createdAt,
                                createdAt.plusSeconds(900), paidAt);
                when(paymentService.simulatePayment(paymentId)).thenReturn(response);

                mockMvc.perform(post("/api/payments/{id}/simulate-payment", paymentId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("PAID"))
                                .andExpect(jsonPath("$.paidAt").value(paidAt.toString()));
                verify(paymentService).simulatePayment(paymentId);
        }

        @Test
        void shouldReturnNotFoundWhenSimulatedPaymentDoesNotExist() throws Exception {
                UUID id = UUID.randomUUID();
                when(paymentService.simulatePayment(id)).thenThrow(
                                new ResourceNotFoundException("Payment not found: " + id));

                mockMvc.perform(post("/api/payments/{id}/simulate-payment", id))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnConflictWhenSimulatedPaymentIsExpired() throws Exception {
                assertSimulationConflict("Expired payment cannot be simulated");
        }

        @Test
        void shouldReturnConflictWhenSimulatedPaymentIsAlreadyPaid() throws Exception {
                assertSimulationConflict("Only pending payments can be simulated");
        }

        private void assertSimulationConflict(String message) throws Exception {
                UUID id = UUID.randomUUID();
                when(paymentService.simulatePayment(id)).thenThrow(
                                new PaymentStateConflictException(message));

                mockMvc.perform(post("/api/payments/{id}/simulate-payment", id))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message").value(message));
        }
        @Test
        void shouldReturnPaginatedPaymentsWithDefaultParameters()
                throws Exception {
                UUID paymentId = UUID.randomUUID();
                Instant createdAt =
                        Instant.parse("2026-07-25T12:00:00Z");

                PaymentHistoryResponse payment =
                        new PaymentHistoryResponse(
                                paymentId,
                                50_000L,
                                "tb1q1234567890abcdef1234567890abcdef",
                                PaymentStatus.PENDING,
                                createdAt,
                                createdAt.plusSeconds(900),
                                null
                        );

                Page<PaymentHistoryResponse> page =
                        new PageImpl<>(
                                List.of(payment),
                                PageRequest.of(0, 10),
                                1
                        );

                when(paymentService.getPayments(0, 10, null))
                        .thenReturn(page);

                mockMvc.perform(get("/api/payments"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content[0].id")
                                .value(paymentId.toString()))
                        .andExpect(jsonPath("$.content[0].amountSats")
                                .value(50_000))
                        .andExpect(jsonPath("$.content[0].status")
                                .value("PENDING"))
                        .andExpect(jsonPath("$.totalElements")
                                .value(1))
                        .andExpect(jsonPath("$.number")
                                .value(0))
                        .andExpect(jsonPath("$.size")
                                .value(10));

                verify(paymentService)
                        .getPayments(0, 10, null);
        }

        @Test
        void shouldUseRequestedPageAndSize() throws Exception {
                Page<PaymentHistoryResponse> page =
                        Page.empty(PageRequest.of(1, 5));

                when(paymentService.getPayments(1, 5, null))
                        .thenReturn(page);

                mockMvc.perform(
                                get("/api/payments")
                                        .param("page", "1")
                                        .param("size", "5")
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.number").value(1))
                        .andExpect(jsonPath("$.size").value(5))
                        .andExpect(jsonPath("$.content").isArray());

                verify(paymentService)
                        .getPayments(1, 5, null);
        }

        @Test
        void shouldFilterPaymentsByPendingStatus()
                throws Exception {
                UUID paymentId = UUID.randomUUID();
                Instant createdAt =
                        Instant.parse("2026-07-25T12:00:00Z");

                PaymentHistoryResponse payment =
                        new PaymentHistoryResponse(
                                paymentId,
                                50_000L,
                                "tb1q1234567890abcdef1234567890abcdef",
                                PaymentStatus.PENDING,
                                createdAt,
                                createdAt.plusSeconds(900),
                                null
                        );

                Page<PaymentHistoryResponse> page =
                        new PageImpl<>(
                                List.of(payment),
                                PageRequest.of(0, 10),
                                1
                        );

                when(paymentService.getPayments(
                        0,
                        10,
                        PaymentStatus.PENDING
                )).thenReturn(page);

                mockMvc.perform(
                                get("/api/payments")
                                        .param("status", "PENDING")
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content[0].status")
                                .value("PENDING"))
                        .andExpect(jsonPath("$.totalElements")
                                .value(1));

                verify(paymentService).getPayments(
                        0,
                        10,
                        PaymentStatus.PENDING
                );
        }

        @Test
        void shouldReturnBadRequestWhenPageIsNegative()
                throws Exception {
                mockMvc.perform(
                                get("/api/payments")
                                        .param("page", "-1")
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.error")
                                .value("Bad Request"))
                        .andExpect(jsonPath("$.message")
                                .value("page must be zero or greater"));

                verifyNoInteractions(paymentService);
        }

        @Test
        void shouldReturnBadRequestWhenSizeIsZero()
                throws Exception {
                mockMvc.perform(
                                get("/api/payments")
                                        .param("size", "0")
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.error")
                                .value("Bad Request"))
                        .andExpect(jsonPath("$.message")
                                .value("size must be greater than zero"));

                verifyNoInteractions(paymentService);
        }

        @Test
        void shouldReturnBadRequestWhenStatusIsInvalid()
                throws Exception {
                mockMvc.perform(
                                get("/api/payments")
                                        .param("status", "INVALID")
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.error")
                                .value("Bad Request"))
                        .andExpect(jsonPath("$.message")
                                .value(
                                        "Invalid value for parameter: status"
                                ));

                verifyNoInteractions(paymentService);
        }
        @Test
        void shouldReturnBadRequestWhenPageIsNotANumber()
                throws Exception {
                mockMvc.perform(
                                get("/api/payments")
                                        .param("page", "abc")
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400))
                        .andExpect(jsonPath("$.message")
                                .value(
                                        "Invalid value for parameter: page"
                                ));

                verifyNoInteractions(paymentService);
        }
}
