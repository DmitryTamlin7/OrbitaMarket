package org.example.dto;

import org.example.entity.OrderStatus;
import org.example.entity.ProductType;

import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        OrderStatus status,
        ProductType productType,
        Integer price,
        Instant createdAt)
{}
