package org.example.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderPaymentRequestedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        Integer amount,
        Instant occurredAt)
{}
