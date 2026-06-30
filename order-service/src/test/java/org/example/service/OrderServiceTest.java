 package org.example.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.dto.*;
import org.example.entity.*;
import org.example.repository.OrderRepository;
import org.example.repository.OutBoxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для OrderService (Сервис заказов)")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OutBoxEventRepository outBoxEventRepository;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OrderService orderService;

    private final String USER_ID = "Dima-01";
    private UUID orderId;
    private Order existingOrder;
    private OrderPayload mockPayload;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();

        Map<String, String> timeWindow = Map.of(
                "start", "2026-06-29T19:30:00",
                "end", "2026-06-29T21:30:00"
        );


        existingOrder = new Order();
        existingOrder.setId(orderId);
        existingOrder.setUserId(USER_ID);
        existingOrder.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        existingOrder.setProductType(ProductType.ARCHIVE);
        existingOrder.setPrice(10);
        existingOrder.setPayload(mockPayload);
        existingOrder.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("Метод: createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("Ошибка: цена равна null — выбросить IllegalArgumentException")
        void createOrder_PriceNull_ThrowsException() {
            CreateOrderRequest mockRequest = mock(CreateOrderRequest.class);
            when(mockRequest.price()).thenReturn(null);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    orderService.createOrder(USER_ID, mockRequest)
            );
            assertEquals("INVALID_PRICE", exception.getMessage());
            verifyNoInteractions(orderRepository, outBoxEventRepository);
        }

        @Test
        @DisplayName("Ошибка: цена <= 0 — выбросить IllegalArgumentException")
        void createOrder_PriceZeroOrLess_ThrowsException() {
            CreateOrderRequest mockRequest = mock(CreateOrderRequest.class);
            when(mockRequest.price()).thenReturn(0);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                    orderService.createOrder(USER_ID, mockRequest)
            );
            assertEquals("INVALID_PRICE", exception.getMessage());
            verifyNoInteractions(orderRepository, outBoxEventRepository);
        }

        @Test
        @DisplayName("Успешное создание заказа TASKING и сохранение события в Outbox")
        void createOrder_Success() throws JsonProcessingException {
            // Arrange
            CreateOrderRequest mockRequest = mock(CreateOrderRequest.class);
            when(mockRequest.price()).thenReturn(10);
            when(mockRequest.productType()).thenReturn(ProductType.TASKING);


            doReturn(mockPayload).when(mockRequest).payload();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order orderToSave = invocation.getArgument(0);
                orderToSave.setId(orderId);
                orderToSave.setCreatedAt(Instant.now());
                return orderToSave;
            });

            String expectedJson = "{\"orderId\":\"" + orderId + "\"}";
            when(objectMapper.writeValueAsString(any(OrderPaymentRequestedEvent.class))).thenReturn(expectedJson);

            // Act
            OrderResponse response = orderService.createOrder(USER_ID, mockRequest);

            // Assert
            assertNotNull(response);
            assertEquals(orderId, response.orderId());
            assertEquals(OrderStatus.PAYMENT_PENDING, response.status());
            assertEquals(ProductType.TASKING, response.productType());

            verify(orderRepository, times(1)).save(any(Order.class));

            ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
            verify(outBoxEventRepository, times(1)).save(outboxCaptor.capture());

            OutboxEvent savedOutboxEvent = outboxCaptor.getValue();
            assertEquals(orderId.toString(), savedOutboxEvent.getAggregateId());
            assertEquals(OutboxEventType.ORDER_PAYMENT_REQUESTED, savedOutboxEvent.getEventType());
            assertEquals(expectedJson, savedOutboxEvent.getPayload());
        }

        @Test
        @DisplayName("Ошибка сериализации ObjectMapper — выбросить RuntimeException")
        void createOrder_SerializationThrowsException() throws JsonProcessingException {
            CreateOrderRequest mockRequest = mock(CreateOrderRequest.class);
            when(mockRequest.price()).thenReturn(10);

            when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

            when(objectMapper.writeValueAsString(any(OrderPaymentRequestedEvent.class)))
                    .thenThrow(new JsonProcessingException("Jackson Error") {});

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    orderService.createOrder(USER_ID, mockRequest)
            );
            assertEquals("Failed Serialize", exception.getMessage());
            verifyNoInteractions(outBoxEventRepository);
        }
    }

    @Nested
    @DisplayName("Метод: processPaymentCompletion")
    class ProcessPaymentCompletionTests {

        @Test
        @DisplayName("Заказ не найден — выбросить EntityNotFoundException")
        void processPaymentCompletion_OrderNotFound() {
            OrderPaymentCompletedEvent mockEvent = mock(OrderPaymentCompletedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    orderService.processPaymentCompletion(mockEvent)
            );
            assertEquals("Заказ не найден id:" + orderId, exception.getMessage());
        }

        @Test
        @DisplayName("Идемпотентность: Заказ уже PAID — ничего не делать")
        void processPaymentCompletion_AlreadyPaid_DoesNothing() {
            OrderPaymentCompletedEvent mockEvent = mock(OrderPaymentCompletedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            existingOrder.setOrderStatus(OrderStatus.PAID);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            orderService.processPaymentCompletion(mockEvent);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Конфликт статусов: Заказ не в PAYMENT_PENDING — не менять статус")
        void processPaymentCompletion_NotPending_DoesNothing() {
            OrderPaymentCompletedEvent mockEvent = mock(OrderPaymentCompletedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            existingOrder.setOrderStatus(OrderStatus.CREATED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            orderService.processPaymentCompletion(mockEvent);

            assertEquals(OrderStatus.CREATED, existingOrder.getOrderStatus());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Успешное подтверждение оплаты — перевести статус в PAID")
        void processPaymentCompletion_Success() {
            OrderPaymentCompletedEvent mockEvent = mock(OrderPaymentCompletedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            orderService.processPaymentCompletion(mockEvent);

            assertEquals(OrderStatus.PAID, existingOrder.getOrderStatus());
            verify(orderRepository, times(1)).save(existingOrder);
        }
    }

    @Nested
    @DisplayName("Метод: processPaymentFailure")
    class ProcessPaymentFailureTests {

        @Test
        @DisplayName("Заказ не найден — выбросить EntityNotFoundException")
        void processPaymentFailure_OrderNotFound() {
            OrderPaymentFailedEvent mockEvent = mock(OrderPaymentFailedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    orderService.processPaymentFailure(mockEvent)
            );
        }

        @Test
        @DisplayName("Идемпотентность: Заказ уже в PAYMENT_FAILED — ничего не делать")
        void processPaymentFailure_AlreadyFailed_DoesNothing() {
            OrderPaymentFailedEvent mockEvent = mock(OrderPaymentFailedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            existingOrder.setOrderStatus(OrderStatus.PAYMENT_FAILED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            orderService.processPaymentFailure(mockEvent);

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Успешная обработка отказа — выставить статус PAYMENT_FAILED")
        void processPaymentFailure_Success() {
            OrderPaymentFailedEvent mockEvent = mock(OrderPaymentFailedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);

            FailureReason expectedReason = FailureReason.INSUFFICIENT_BALANCE;
            when(mockEvent.reason()).thenReturn(expectedReason);

            when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

            orderService.processPaymentFailure(mockEvent);

            assertEquals(OrderStatus.PAYMENT_FAILED, existingOrder.getOrderStatus());
            assertEquals(expectedReason, existingOrder.getFailureReason());
            verify(orderRepository, times(1)).save(existingOrder);
        }
    }

    @Nested
    @DisplayName("Метод: getAllOrders")
    class GetAllOrdersTests {

        @Test
        @DisplayName("Вернуть список заказов для пользователя")
        void getAllOrders_ReturnsList() {
            List<Order> mockList = Collections.singletonList(existingOrder);
            when(orderRepository.findByUserId(USER_ID)).thenReturn(mockList);

            List<Order> result = orderService.getAllOrders(USER_ID);

            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            assertEquals(ProductType.ARCHIVE, result.get(0).getProductType());
        }
    }

    @Nested
    @DisplayName("Метод: getStatusOrder")
    class GetStatusOrderTests {

        @Test
        @DisplayName("Заказ не найден — выбросить EntityNotFoundException (ORDER_NOT_FOUND)")
        void getStatusOrder_NotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () ->
                    orderService.getStatusOrder(orderId)
            );
            assertEquals("ORDER_NOT_FOUND", exception.getMessage());
        }

    }
}