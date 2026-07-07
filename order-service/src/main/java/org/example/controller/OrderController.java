package org.example.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.example.dto.CreateOrderRequest;
import org.example.dto.ErrorResponse;
import org.example.dto.OrderResponse;
import org.example.dto.OrderStatusResponse;
import org.example.entity.Order;
import org.example.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@AllArgsConstructor
@Tag(name = "Order Controller", description = "Управление заказами пользователей")
public class OrderController {

    private final OrderService orderService;


    @PostMapping
    @Operation(summary = "Создание заказа", description = "Создает новый заказ  для указанного пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Заказ успешно создан"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации запроса: \n" +
                    "- INVALID_PAYLOAD: Нет обязательных полей в payload \n" +
                    "- INVALID_PRICE: Цена заказа <= 0 \n" +
                    "- UNKNOWN_PRODUCT_TYPE: Неподдерживаемый тип продукта \n" +
                    "- MISSING_USER_ID: Отсутствует заголовок X-User-Id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "ID пользователя", required = true)
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateOrderRequest request)
    {
        OrderResponse response = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    @Operation(summary = "Получить все заказы пользователя", description = "Возвращает список всех заказов, принадлежащих пользователю из заголовка")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список заказов успешно получен"),
            @ApiResponse(responseCode = "400", description = "MISSING_USER_ID: Нет идентификатора пользователя в заголовках",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<Order>> getOrdersById(
            @Parameter(description = "ID пользователя для фильтрации", required = true)
            @RequestHeader("X-User-Id") String userId){
        List<Order> orderList = orderService.getAllOrders(userId);
        return ResponseEntity.ok(orderList);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Получить статус заказа", description = "Возвращает текущий статус конкретного заказа по его UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Статус заказа успешно получен"),
            @ApiResponse(responseCode = "400", description = "MISSING_USER_ID: Нет идентификатора пользователя в заголовках",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND: Заказ не найден или принадлежит другому пользователю",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderStatusResponse> orderDitails(
            @Parameter(description = "UUID заказа", required = true)
            @PathVariable UUID orderId){
        OrderStatusResponse response = orderService.getStatusOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
