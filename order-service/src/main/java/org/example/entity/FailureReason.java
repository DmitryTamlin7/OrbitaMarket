package org.example.entity;

/**
 * Варинаты статуса неудачной оплаты
 */
public enum FailureReason {
    INSUFFICIENT_BALANCE,
    INVALID_PAYLOAD,
    INVALID_PRICE
}
