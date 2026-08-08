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
import com.bitcoinpaymentgateway.backend.error.PaymentStateConflictException;
import com.bitcoinpaymentgateway.backend.dto.PaymentHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

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

        @Test
        void shouldSimulatePendingPayment() {
                UUID id = UUID.randomUUID();
                Payment payment = createSimulatablePayment(id);
                when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(payment));
                when(paymentRepository.save(payment)).thenReturn(payment);

                Instant before = Instant.now();
                PaymentResponse response = paymentService.simulatePayment(id);

                assertEquals(PaymentStatus.PAID, response.status());
                assertNotNull(response.paidAt());
                assertTrue(!response.paidAt().isBefore(before));
                assertEquals(PaymentStatus.PAID, payment.getStatus());
                verify(paymentRepository).findByIdForUpdate(id);
                verify(paymentRepository).save(payment);
        }

        @Test
        void shouldThrowWhenSimulatedPaymentIsMissing() {
                UUID id = UUID.randomUUID();
                when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

                assertThrows(
                        ResourceNotFoundException.class,
                        () -> paymentService.simulatePayment(id)
                );
                verify(paymentRepository, never()).save(any());
        }

        @Test
        void shouldRejectExpiredPayment() {
                UUID id = UUID.randomUUID();
                Payment payment = createSimulatablePayment(id);
                payment.setStatus(PaymentStatus.EXPIRED);
                when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(payment));

                assertThrows(
                        PaymentStateConflictException.class,
                        () -> paymentService.simulatePayment(id)
                );
                assertNull(payment.getPaidAt());
                verify(paymentRepository, never()).save(any());
        }

        @Test
        void shouldExpireAndRejectPendingPaymentPastExpiration() {
                UUID id = UUID.randomUUID();
                Payment payment = createSimulatablePayment(id);
                payment.setExpiresAt(Instant.now().minusSeconds(1));
                when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(payment));
                when(paymentRepository.save(payment)).thenReturn(payment);

                assertThrows(
                        PaymentStateConflictException.class,
                        () -> paymentService.simulatePayment(id)
                );
                assertEquals(PaymentStatus.EXPIRED, payment.getStatus());
                assertNull(payment.getPaidAt());
                verify(paymentRepository).save(payment);
        }

        @Test
        void shouldRejectAlreadyPaidPayment() {
                UUID id = UUID.randomUUID();
                Payment payment = createSimulatablePayment(id);
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(Instant.now().minusSeconds(1));
                when(paymentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(payment));

                assertThrows(
                        PaymentStateConflictException.class,
                        () -> paymentService.simulatePayment(id)
                );
                verify(paymentRepository, never()).save(any());
        }
        @Test
        void getPaymentsWithoutStatusShouldUseFindAll() {
                Payment payment = createHistoryPayment(
                        PaymentStatus.PENDING,
                        Instant.parse("2026-07-25T12:00:00Z")
                );

                Page<Payment> repositoryPage =
                        new PageImpl<>(List.of(payment));

                when(paymentRepository.findAll(any(Pageable.class)))
                        .thenReturn(repositoryPage);

                Page<PaymentHistoryResponse> result =
                        paymentService.getPayments(0, 10, null);

                ArgumentCaptor<Pageable> pageableCaptor =
                        ArgumentCaptor.forClass(Pageable.class);

                verify(paymentRepository)
                        .findAll(pageableCaptor.capture());

                verify(paymentRepository, never())
                        .findByStatus(
                                any(PaymentStatus.class),
                                any(Pageable.class)
                        );

                assertEquals(1, result.getTotalElements());
                assertEquals(0, pageableCaptor.getValue().getPageNumber());
                assertEquals(10, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void getPaymentsWithStatusShouldUseStatusQuery() {
                Payment payment = createHistoryPayment(
                        PaymentStatus.PENDING,
                        Instant.parse("2026-07-25T12:00:00Z")
                );

                Page<Payment> repositoryPage =
                        new PageImpl<>(List.of(payment));

                when(paymentRepository.findByStatus(
                        eq(PaymentStatus.PENDING),
                        any(Pageable.class)
                )).thenReturn(repositoryPage);

                Page<PaymentHistoryResponse> result =
                        paymentService.getPayments(
                                0,
                                10,
                                PaymentStatus.PENDING
                        );

                ArgumentCaptor<Pageable> pageableCaptor =
                        ArgumentCaptor.forClass(Pageable.class);

                verify(paymentRepository).findByStatus(
                        eq(PaymentStatus.PENDING),
                        pageableCaptor.capture()
                );

                verify(paymentRepository, never())
                        .findAll(any(Pageable.class));

                assertEquals(1, result.getTotalElements());
                assertEquals(
                        PaymentStatus.PENDING,
                        result.getContent().get(0).status()
                );
        }

        @Test
        void getPaymentsShouldSortByCreatedAtDescending() {
                when(paymentRepository.findAll(any(Pageable.class)))
                        .thenReturn(Page.empty());

                paymentService.getPayments(0, 10, null);

                ArgumentCaptor<Pageable> pageableCaptor =
                        ArgumentCaptor.forClass(Pageable.class);

                verify(paymentRepository)
                        .findAll(pageableCaptor.capture());

                Pageable pageable = pageableCaptor.getValue();

                Sort.Order createdAtOrder =
                        pageable.getSort().getOrderFor("createdAt");

                assertEquals(0, pageable.getPageNumber());
                assertEquals(10, pageable.getPageSize());
                assertNotNull(createdAtOrder);
                assertEquals(
                        Sort.Direction.DESC,
                        createdAtOrder.getDirection()
                );
        }

        @Test
        void getPaymentsShouldMapEntitiesToDtos() {
                Instant createdAt =
                        Instant.parse("2026-07-25T12:00:00Z");

                Payment payment = createHistoryPayment(
                        PaymentStatus.PAID,
                        createdAt
                );

                when(paymentRepository.findAll(any(Pageable.class)))
                        .thenReturn(new PageImpl<>(List.of(payment)));

                Page<PaymentHistoryResponse> result =
                        paymentService.getPayments(0, 10, null);

                PaymentHistoryResponse response =
                        result.getContent().get(0);

                assertEquals(payment.getId(), response.id());
                assertEquals(payment.getAmountSats(), response.amountSats());
                assertEquals(
                        payment.getBitcoinAddress(),
                        response.bitcoinAddress()
                );
                assertEquals(payment.getStatus(), response.status());
                assertEquals(payment.getCreatedAt(), response.createdAt());
                assertEquals(payment.getExpiresAt(), response.expiresAt());
                assertEquals(payment.getPaidAt(), response.paidAt());
        }

        @Test
        void getPaymentsShouldReturnEmptyPage() {
                when(paymentRepository.findAll(any(Pageable.class)))
                        .thenReturn(Page.empty());

                Page<PaymentHistoryResponse> result =
                        paymentService.getPayments(0, 10, null);

                assertTrue(result.isEmpty());
                assertEquals(0, result.getTotalElements());
        }
        private Payment createHistoryPayment(
                PaymentStatus status,
                Instant createdAt
        ) {
                return Payment.builder()
                        .id(UUID.randomUUID())
                        .amountSats(50_000L)
                        .bitcoinAddress(TEST_ADDRESS)
                        .status(status)
                        .createdAt(createdAt)
                        .expiresAt(createdAt.plusSeconds(900))
                        .paidAt(
                                status == PaymentStatus.PAID
                                        ? createdAt.plusSeconds(60)
                                        : null
                        )
                        .build();
        }

        private Payment createSimulatablePayment(UUID id) {
                Instant createdAt = Instant.now();
                return Payment.builder()
                        .id(id)
                        .amountSats(50_000L)
                        .bitcoinAddress(TEST_ADDRESS)
                        .status(PaymentStatus.PENDING)
                        .createdAt(createdAt)
                        .expiresAt(createdAt.plusSeconds(900))
                        .build();
        }
}
