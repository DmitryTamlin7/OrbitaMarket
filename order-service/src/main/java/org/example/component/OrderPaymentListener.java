package org.example.component;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.OrderPaymentCompletedEvent;
import org.example.dto.OrderPaymentFailedEvent;
import org.example.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Асинхронный потребитель сообщений (Kafka Message Consumer) платежного домена.
 * Отвечает за обработку результатов процессинга биллинга и
 * вызывает метод нужного статуса
 */
@Slf4j
@Component
@AllArgsConstructor
public class OrderPaymentListener {

    private final String TOPIC_COMPLETE = "payment-completed-events";
    private final String TOPIC_FAILED = "payment-failed-events";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Обработчик событий успешного списания денежных средств со счета пользователя.
     * Десериализует входящий payload и запускает процесс финализации статуса сделки (PAID).
     */
    @KafkaListener(topics = TOPIC_COMPLETE, groupId = "orders-service-group")
    public void onPaymentComplete(String message){
        try {
            OrderPaymentCompletedEvent completedEvent = objectMapper.readValue(message, OrderPaymentCompletedEvent.class);
            log.info("Получено сообщение об успешной оплате заказа: {}", completedEvent.orderId());
            orderService.processPaymentCompletion(completedEvent);
        }catch (Exception e) {
            log.error("Ошибка при получении успешной оплаты: {} ", e.getMessage());
        }
    }

    /**
     * Обработчик событий отклонения транзакции или нехватки баланса.
     * Десериализует payload ошибки и фиксирует статус PAYMENT_FAILED с записью причины в лог и БД.
     */
    @KafkaListener(topics = TOPIC_FAILED, groupId = "orders-service-group" )
    public void onPaymentFailure(String message){
        try {
            OrderPaymentFailedEvent failedEvent = objectMapper.readValue(message, OrderPaymentFailedEvent.class);
            log.info("Получено сообщение об ошибке при оплате, Причина: " + failedEvent.reason());
            orderService.processPaymentFailure(failedEvent);
        }catch (Exception e){
            log.error("Ошибка при обработке ошибки оплаты: {}", e.getMessage());
        }

    }
}
