package com.bitcoinpaymentgateway.backend.service;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import com.bitcoinpaymentgateway.backend.dto.CreatePaymentRequest;
import com.bitcoinpaymentgateway.backend.dto.PaymentHistoryResponse;
import com.bitcoinpaymentgateway.backend.dto.PaymentResponse;
import com.bitcoinpaymentgateway.backend.error.ResourceNotFoundException;
import com.bitcoinpaymentgateway.backend.error.PaymentStateConflictException;
import com.bitcoinpaymentgateway.backend.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Duration PAYMENT_EXPIRATION = Duration.ofMinutes(15);

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

    @Transactional
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));

        expireIfNeeded(payment, Instant.now());

        return PaymentResponse.from(payment);
    }

    @Transactional(noRollbackFor = PaymentStateConflictException.class)
    public PaymentResponse simulatePayment(UUID paymentId) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found: " + paymentId));

        Instant now = Instant.now();
        if (expireIfNeeded(payment, now)) {
            throw new PaymentStateConflictException(
                    "Expired payment cannot be simulated: " + paymentId);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentStateConflictException(
                    "Only pending payments can be simulated: " + paymentId);
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        Payment savedPayment = paymentRepository.save(payment);

        return PaymentResponse.from(savedPayment);
    }

    private boolean expireIfNeeded(Payment payment, Instant now) {
        if (payment.getStatus() == PaymentStatus.PENDING
                && now.isAfter(payment.getExpiresAt())) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            return true;
        }
        return payment.getStatus() == PaymentStatus.EXPIRED;
    }

    @Transactional(readOnly = true)
    public Page<PaymentHistoryResponse> getPayments(
            int page,
            int size,
            PaymentStatus status
    ) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Payment> payments = status == null
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(status, pageable);

        return payments.map(PaymentHistoryResponse::from);
    }
}
