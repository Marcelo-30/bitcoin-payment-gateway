package com.bitcoinpaymentgateway.backend.service;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.CreatePaymentRequest;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Duration PAYMENT_EXPIRATION =
            Duration.ofMinutes(15);

    private final PaymentRepository paymentRepository;
    private final BitcoinAddressGenerator bitcoinAddressGenerator;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Instant createdAt = Instant.now();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .amountSats(request.amountSats())
                .bitcoinAddress(bitcoinAddressGenerator.generate())
                .status(PaymentStatus.PENDING)
                .createdAt(createdAt)
                .expiresAt(createdAt.plus(PAYMENT_EXPIRATION))
                .paidAt(null)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponse.from(savedPayment);
    }
}
