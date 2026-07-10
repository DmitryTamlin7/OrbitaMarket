package org.example.entity;

/**
 * Перечисление стадий обработки заказа.
 */
public enum OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAID,
    PAYMENT_FAILED,
    REJECTED
}
