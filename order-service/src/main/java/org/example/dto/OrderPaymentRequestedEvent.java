package org.example.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.Instant;
import java.util.UUID;


public record OrderPaymentRequestedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        Integer amount,
        Instant occurred_at
) {}
