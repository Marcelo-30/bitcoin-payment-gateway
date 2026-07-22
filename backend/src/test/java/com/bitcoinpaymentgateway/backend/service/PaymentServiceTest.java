package com.bitcoinpaymentgateway.backend.service;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.CreatePaymentRequest;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bitcoinpaymentgateway.backend.error.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

        private static final String TEST_ADDRESS = "tb1q1234567890abcdef1234567890abcdef";

        @Mock
        private PaymentRepository paymentRepository;

        @Mock
        private BitcoinAddressGenerator bitcoinAddressGenerator;

        @InjectMocks
        private PaymentService paymentService;

        @Test
        void shouldCreateAndSavePendingPayment() {
                CreatePaymentRequest request = new CreatePaymentRequest(50_000L);

                when(bitcoinAddressGenerator.generate())
                                .thenReturn(TEST_ADDRESS);

                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PaymentResponse response = paymentService.createPayment(request);

                ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

                verify(paymentRepository).save(paymentCaptor.capture());

                Payment savedPayment = paymentCaptor.getValue();

                assertNotNull(savedPayment.getId());
                assertEquals(50_000L, savedPayment.getAmountSats());
                assertEquals(TEST_ADDRESS, savedPayment.getBitcoinAddress());
                assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
                assertNotNull(savedPayment.getCreatedAt());
                assertNotNull(savedPayment.getExpiresAt());
                assertNull(savedPayment.getPaidAt());

                assertEquals(
                                Duration.ofMinutes(15),
                                Duration.between(
                                                savedPayment.getCreatedAt(),
                                                savedPayment.getExpiresAt()));

                assertEquals(savedPayment.getId(), response.id());
        }

        @Test
        void shouldUseGeneratedBitcoinAddress() {
                CreatePaymentRequest request = new CreatePaymentRequest(25_000L);

                when(bitcoinAddressGenerator.generate())
                                .thenReturn(TEST_ADDRESS);

                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PaymentResponse response = paymentService.createPayment(request);

                assertEquals(TEST_ADDRESS, response.bitcoinAddress());
                verify(bitcoinAddressGenerator).generate();
        }

        @Test
        void shouldReturnPaymentWhenNotExpired() {
                UUID id = UUID.randomUUID();
                Instant createdAt = Instant.now();
                Payment payment = Payment.builder()
                                .id(id)
                                .amountSats(50_000L)
                                .bitcoinAddress(TEST_ADDRESS)
                                .status(PaymentStatus.PENDING)
                                .createdAt(createdAt)
                                .expiresAt(createdAt.plusSeconds(900))
                                .build();

                when(paymentRepository.findById(id))
                                .thenReturn(Optional.of(payment));

                PaymentResponse response = paymentService.getPayment(id);

                assertEquals(PaymentStatus.PENDING, response.status());
                verify(paymentRepository, never()).save(any());
        }

        @Test
        void shouldMarkAsExpiredWhenPastExpiration() {
                UUID id = UUID.randomUUID();
                Instant createdAt = Instant.now().minusSeconds(1000);
                Payment payment = Payment.builder()
                                .id(id)
                                .amountSats(50_000L)
                                .bitcoinAddress(TEST_ADDRESS)
                                .status(PaymentStatus.PENDING)
                                .createdAt(createdAt)
                                .expiresAt(createdAt.plusSeconds(900))
                                .build();

                when(paymentRepository.findById(id))
                                .thenReturn(Optional.of(payment));
                when(paymentRepository.save(any(Payment.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PaymentResponse response = paymentService.getPayment(id);

                assertEquals(PaymentStatus.EXPIRED, response.status());
                verify(paymentRepository).save(payment);
        }

        @Test
        void shouldThrowWhenPaymentNotFound() {
                UUID id = UUID.randomUUID();
                when(paymentRepository.findById(id))
                                .thenReturn(Optional.empty());

                ResourceNotFoundException exception = assertThrows(
                                ResourceNotFoundException.class,
                                () -> paymentService.getPayment(id));

                assertEquals(
                                "Payment not found: " + id,
                                exception.getMessage());
        }
}
