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

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String TEST_ADDRESS =
            "tb1q1234567890abcdef1234567890abcdef";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BitcoinAddressGenerator bitcoinAddressGenerator;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void shouldCreateAndSavePendingPayment() {
        CreatePaymentRequest request =
                new CreatePaymentRequest(50_000L);

        when(bitcoinAddressGenerator.generate())
                .thenReturn(TEST_ADDRESS);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.createPayment(request);

        ArgumentCaptor<Payment> paymentCaptor =
                ArgumentCaptor.forClass(Payment.class);

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
                        savedPayment.getExpiresAt()
                )
        );

        assertEquals(savedPayment.getId(), response.id());
    }

    @Test
    void shouldUseGeneratedBitcoinAddress() {
        CreatePaymentRequest request =
                new CreatePaymentRequest(25_000L);

        when(bitcoinAddressGenerator.generate())
                .thenReturn(TEST_ADDRESS);

        when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response =
                paymentService.createPayment(request);

        assertEquals(TEST_ADDRESS, response.bitcoinAddress());
        verify(bitcoinAddressGenerator).generate();
    }
}
