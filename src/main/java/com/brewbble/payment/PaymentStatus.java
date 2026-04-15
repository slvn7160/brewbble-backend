package com.brewbble.payment;

public enum PaymentStatus {
    UNPAID,
    PENDING,   // payment initiated but not yet confirmed
    PAID,
    FAILED,
    REFUNDED
}
