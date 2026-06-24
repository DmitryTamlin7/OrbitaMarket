package org.example.dto;

import java.util.UUID;

public record OrderPaymentCompletedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        Integer amount,
        Integer newBalance
) {}
