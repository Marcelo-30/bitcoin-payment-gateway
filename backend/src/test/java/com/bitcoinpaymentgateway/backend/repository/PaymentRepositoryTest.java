package com.bitcoinpaymentgateway.backend.repository;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PaymentRepositoryTest {

    private static final String TEST_ADDRESS =
            "tb1q1234567890abcdef1234567890abcdef";

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void shouldReturnPaymentsSortedByCreatedAtDescending() {
        Instant baseTime = Instant.parse("2026-07-25T12:00:00Z");

        Payment oldest = createPayment(
                10_000L,
                PaymentStatus.PENDING,
                baseTime
        );

        Payment newest = createPayment(
                30_000L,
                PaymentStatus.PAID,
                baseTime.plusSeconds(120)
        );

        Payment middle = createPayment(
                20_000L,
                PaymentStatus.EXPIRED,
                baseTime.plusSeconds(60)
        );

        paymentRepository.saveAllAndFlush(
                List.of(oldest, newest, middle)
        );

        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> result =
                paymentRepository.findAll(pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(newest.getId(), result.getContent().get(0).getId());
        assertEquals(middle.getId(), result.getContent().get(1).getId());
        assertEquals(oldest.getId(), result.getContent().get(2).getId());
    }

    @Test
    void shouldLimitFirstPageToRequestedSize() {
        Instant baseTime = Instant.parse("2026-07-25T12:00:00Z");

        paymentRepository.saveAllAndFlush(List.of(
                createPayment(
                        10_000L,
                        PaymentStatus.PENDING,
                        baseTime
                ),
                createPayment(
                        20_000L,
                        PaymentStatus.PENDING,
                        baseTime.plusSeconds(60)
                ),
                createPayment(
                        30_000L,
                        PaymentStatus.PAID,
                        baseTime.plusSeconds(120)
                )
        ));

        PageRequest pageable = PageRequest.of(
                0,
                2,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> result =
                paymentRepository.findAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(3, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.hasNext());
    }

    @Test
    void shouldFilterPaymentsByPendingStatus() {
        Instant baseTime = Instant.parse("2026-07-25T12:00:00Z");

        paymentRepository.saveAllAndFlush(List.of(
                createPayment(
                        10_000L,
                        PaymentStatus.PENDING,
                        baseTime
                ),
                createPayment(
                        20_000L,
                        PaymentStatus.PENDING,
                        baseTime.plusSeconds(60)
                ),
                createPayment(
                        30_000L,
                        PaymentStatus.PAID,
                        baseTime.plusSeconds(120)
                )
        ));

        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> result =
                paymentRepository.findByStatus(
                        PaymentStatus.PENDING,
                        pageable
                );

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(payment ->
                        payment.getStatus() == PaymentStatus.PENDING
                ));
    }

    @Test
    void shouldFilterPaymentsByPaidStatus() {
        Instant baseTime = Instant.parse("2026-07-25T12:00:00Z");

        paymentRepository.saveAllAndFlush(List.of(
                createPayment(
                        10_000L,
                        PaymentStatus.PENDING,
                        baseTime
                ),
                createPayment(
                        20_000L,
                        PaymentStatus.PAID,
                        baseTime.plusSeconds(60)
                ),
                createPayment(
                        30_000L,
                        PaymentStatus.PAID,
                        baseTime.plusSeconds(120)
                )
        ));

        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> result =
                paymentRepository.findByStatus(
                        PaymentStatus.PAID,
                        pageable
                );

        assertEquals(2, result.getTotalElements());
        assertTrue(result.getContent().stream()
                .allMatch(payment ->
                        payment.getStatus() == PaymentStatus.PAID
                ));
    }

    @Test
    void shouldReturnEmptyPageWhenNoPaymentsExist() {
        PageRequest pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> result =
                paymentRepository.findAll(pageable);

        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    private Payment createPayment(
            long amountSats,
            PaymentStatus status,
            Instant createdAt
    ) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .amountSats(amountSats)
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
}