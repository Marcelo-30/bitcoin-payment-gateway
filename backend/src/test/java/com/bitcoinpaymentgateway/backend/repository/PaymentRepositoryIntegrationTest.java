package com.bitcoinpaymentgateway.backend.repository;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class PaymentRepositoryIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void shouldSavePaymentSuccessfully() {
        Payment payment = createPayment(50_000L);

        Payment savedPayment = paymentRepository.saveAndFlush(payment);

        assertEquals(payment.getId(), savedPayment.getId());
        assertEquals(50_000L, savedPayment.getAmountSats());
        assertEquals(PaymentStatus.PENDING, savedPayment.getStatus());
    }

    @Test
    void shouldFindPaymentByUuid() {
        Payment payment = createPayment(25_000L);
        paymentRepository.saveAndFlush(payment);

        Optional<Payment> foundPayment =
                paymentRepository.findById(payment.getId());

        assertTrue(foundPayment.isPresent());
        assertEquals(payment.getId(), foundPayment.get().getId());
        assertEquals(
                payment.getBitcoinAddress(),
                foundPayment.get().getBitcoinAddress()
        );
    }

    @Test
    void shouldRejectPaymentWithNonPositiveAmount() {
        Payment invalidPayment = createPayment(0L);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> paymentRepository.saveAndFlush(invalidPayment)
        );
    }

    private Payment createPayment(Long amountSats) {
        Instant createdAt = Instant.now();

        return Payment.builder()
                .id(UUID.randomUUID())
                .amountSats(amountSats)
                .bitcoinAddress("tb1qexamplebitcoinaddress")
                .status(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .expiresAt(createdAt.plusSeconds(900))
                .paidAt(null)
                .build();
    }
}