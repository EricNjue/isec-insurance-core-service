package com.isec.platform.common.exception;

public class PaymentNotFoundException extends ResourceNotFoundException {
    public PaymentNotFoundException(String checkoutRequestId) {
        super("Payment not found for checkoutRequestId: " + checkoutRequestId);
    }
}
