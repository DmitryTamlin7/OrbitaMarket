package org.example.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.entity.Order;
import org.example.entity.OrderStatus;
import org.example.entity.OutboxEvent;
import org.example.entity.OutboxEventType;
import org.example.repository.OrderRepository;
import org.example.repository.OutBoxEventRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Сервис оркестрации жизненного цикла заказов  OrbitaMarket
 * Обеспечивает управление сделками ДЗЗ, гарантированную доставку событий
 * через паттерн Transactional Outbox и резидентное кэширование данных.
 */
@Slf4j
@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutBoxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;


    /**
     * Регистрация нового заказа и инициализация асинхронного процесса биллинга.
     * Валидирует ценообразование, сохраняет сущность в БД и фиксирует ивент оплаты
     * в Outbox-таблице в рамках единой ACID-транзакции. Результат кэшируется (Pre-heating).
     *
     * @param userId  Идентификатор авторизованного пользователя (из заголовка X-User-Id)
     * @param request Полиморфный payload параметров спутниковой съемки и метаданных
     * @return DTO с подтверждением успешной регистрации и статусом PAYMENT_PENDING
     */
    @CachePut(value = "orders", key = "#result.id")
    @Transactional
    public OrderResponse createOrder(String userId, CreateOrderRequest request){
        if (request.price() == null || request.price() <= 0){
            throw new IllegalArgumentException("INVALID_PRICE");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setProductType(request.productType());
        order.setPrice(request.price());
        order.setPayload(request.payload());
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        Order savedOrder = orderRepository.save(order);

        OrderPaymentRequestedEvent orderPayEvent = new OrderPaymentRequestedEvent(
                UUID.randomUUID(),
                savedOrder.getId(),
                savedOrder.getUserId(),
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


    /**
     * Асинхронная обработка успешного прохождения биллинга из брокера событий.
     * Верифицирует текущее состояние конечного автомата (State Machine) заказа,
     * переводит его в статус PAID и инвалидирует устаревший кэш (Cache Eviction).
     */
    @CacheEvict(value = "orders", key = "#event.orderId()")
    @Transactional
    public void processPaymentCompletion(OrderPaymentCompletedEvent event){
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден id:" + event.orderId()));

        if (order.getOrderStatus().equals(OrderStatus.PAID)){
            log.info("Заказ уже оплачен");
            return;
        }

        if (!order.getOrderStatus().equals(OrderStatus.PAYMENT_PENDING)){
            log.info("Заказ из статуса {} невозможно оплатить", order.getOrderStatus());
            return;
        }

        order.setOrderStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Статус заказа PAID. Заказ {} созранен", order.getId());
    }


    /**
     * Асинхронная обработка падения или отклонения транзакции платежной системой.
     * Переводит заказ в финальное состояние PAYMENT_FAILED, фиксирует причину отказа
     * (например, INSUFFICIENT_BALANCE) и очищает кэш.
     */
    @CacheEvict(value = "orders", key = "#event.orderId()")
    @Transactional
    public void processPaymentFailure(OrderPaymentFailedEvent event){
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден id:" + event.orderId()));

        if (order.getOrderStatus().equals(OrderStatus.PAYMENT_FAILED)){
            log.info("Заказ уже в статусе PAYMENT_FAILED");
            return;
        }

        order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        order.setFailureReason(event.reason());
        orderRepository.save(order);
        log.info("Статус заказа: {} Причина: {} ", order.getOrderStatus(), order.getFailureReason());
    }


    /**
     * Получение истории всех заказов пользователя.
     * Выполняется в неблокирующей транзакции (Read-Only) для минимизации нагрузки на СУБД.
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders(String userId){
        return orderRepository.findByUserId(userId);
    }



    /**
     * Запрос актуального статуса конкретного заказа.
     * Реализует паттерн ленивого чтения (Cache-Aside): при промахе кэша подтягивает
     * данные прямо из PostgreSQL.
     */
    @Transactional(readOnly = true)
    public OrderStatusResponse getStatusOrder(UUID orderId){
        Order curOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("ORDER_NOT_FOUND"));

        return  new OrderStatusResponse(
                curOrder.getId(),
                curOrder.getOrderStatus(),
                curOrder.getProductType(),
                curOrder.getPrice(),
                curOrder.getFailureReason(),
                curOrder.getCreatedAt()
        );
    }

}
