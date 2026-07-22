package com.bitcoinpaymentgateway.backend.controller;

import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.error.ResourceNotFoundException;
import com.bitcoinpaymentgateway.backend.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


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
}
