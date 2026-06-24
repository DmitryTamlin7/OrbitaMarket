package org.example.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.example.dto.CreateOrderRequest;
import org.example.dto.OrderPaymentRequestedEvent;
import org.example.dto.OrderResponse;
import org.example.entity.Order;
import org.example.entity.OrderStatus;
import org.example.entity.OutboxEvent;
import org.example.entity.OutboxEventType;
import org.example.repository.OrderRepository;
import org.example.repository.OutBoxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private OrderRepository orderRepository;
    private OutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request){
        if (request.price() == null || request.price() <= 0){
            throw new IllegalArgumentException("INVALID_PRICE");
        }

        Order order = new Order();
        order.setUseId(userId);
        order.setProductType(request.productType());
        order.setPrice(request.price());
        order.setPayload(request.payload());
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        Order savedOrder = orderRepository.save(order);

        OrderPaymentRequestedEvent orderPayEvent = new OrderPaymentRequestedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getUseId(),
                savedOrder.getPrice(),
                Instant.now()
        );

        try {
            String eventJson = objectMapper.writeValueAsString(orderPayEvent);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateId(savedOrder.getId().toString());
            outboxEvent.setEventType(OutboxEventType.ORDER_PAYMENT_REQUESTED);
            outboxEvent.setPayload(eventJson);

            outBoxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e){
            throw new RuntimeException("Failed Serialize");
        }
        return  new OrderResponse(
                savedOrder.getId(),
                savedOrder.getOrderStatus(),
                savedOrder.getProductType(),
                savedOrder.getPrice(),
                savedOrder.getCreatedAt()
        );
    }
}
