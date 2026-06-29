package org.example.dto;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.UUID;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrderPaymentFailedEvent(
        UUID eventId,
        UUID orderId,
        String userId,
        FailureReason reason
) {
}
