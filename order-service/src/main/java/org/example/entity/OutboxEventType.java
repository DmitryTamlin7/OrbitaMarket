package org.example.entity;

/**
 * Классификатор доменных событий распределенной системы для маршрутизации внутри Outbox.
 */
public enum OutboxEventType{
    ORDER_PAYMENT_REQUESTED,
    ORDER_CANCELLED,
    ORDER_COMPLETED
}
