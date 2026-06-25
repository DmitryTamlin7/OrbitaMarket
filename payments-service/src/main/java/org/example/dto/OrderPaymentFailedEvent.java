package org.example.dto;
import java.util.UUID;

public record OrderPaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        FailureReason reason
) {
}
