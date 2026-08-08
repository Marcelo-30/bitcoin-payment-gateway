package com.bitcoinpaymentgateway.backend.repository;

import com.bitcoinpaymentgateway.backend.domain.Payment;
import com.bitcoinpaymentgateway.backend.domain.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Page<Payment> findByStatus(
            PaymentStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
}
