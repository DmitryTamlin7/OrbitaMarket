package org.example.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderPaymentRequestedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        Integer amount,
        Instant occurredAt)
{}
