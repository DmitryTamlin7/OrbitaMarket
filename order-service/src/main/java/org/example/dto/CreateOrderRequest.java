package org.example.dto;

import org.example.entity.OrderPayload;
import org.example.entity.ProductType;

public record CreateOrderRequest(
        ProductType productType,
        Integer price,
        OrderPayload payload)
{}
