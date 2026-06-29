package org.example.dto;

import org.example.entity.FailureReason;
import org.example.entity.OrderStatus;
import org.example.entity.ProductType;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusResponse(
        UUID orderId,
        OrderStatus status,
        ProductType productType,
        Integer price,
        FailureReason failureReason,
        Instant createdAt)
{}
