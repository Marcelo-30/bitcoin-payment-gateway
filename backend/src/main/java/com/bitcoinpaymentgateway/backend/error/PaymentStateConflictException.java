package com.bitcoinpaymentgateway.backend.error;

/**
 * Thrown when an operation cannot be applied to a payment in its current state.
 */
public class PaymentStateConflictException extends RuntimeException {

    public PaymentStateConflictException(String message) {
        super(message);
    }
}
