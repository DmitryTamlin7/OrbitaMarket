package org.example.component;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.OrderPaymentCompletedEvent;
import org.example.dto.OrderPaymentFailedEvent;
import org.example.service.OrderService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class OrderPaymentListener {


    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-complete-event", groupId = "orders-service-group")
    public void onPaymentComplete(String message){
        try {
            log.info("Получено сообщение об успешной оплате: {}", message);
            OrderPaymentCompletedEvent completedEvent = objectMapper.readValue(message, OrderPaymentCompletedEvent.class);
            orderService.processPaymentCompletion(completedEvent);
        }catch (Exception e) {
            log.error("Ошибка при получении успешной оплаты: {} ", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment-failure-event", groupId = "orders-service-group" )
    public void onPaymentFailure(String message){
        try {
            log.info("Получено сообщение об ошибке при оплате: " + message);
            OrderPaymentFailedEvent failedEvent = objectMapper.readValue(message, OrderPaymentFailedEvent.class);
            orderService.processPaymentFailure(failedEvent);
        }catch (Exception e){
            log.error("Ошибка при обработке ошибки оплаты: {}", e.getMessage());
        }

    }
}
