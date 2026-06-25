package org.example.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.OrderPaymentRequestedEvent;
import org.example.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class RequestListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-payment-request", groupId = "payment-service")
    public void onPaymentRequested(String massage){
        try {
            log.info("Получен запрос на оплату");
            OrderPaymentRequestedEvent event = objectMapper.readValue(massage, OrderPaymentRequestedEvent.class);
            paymentService.processPayment(event);
        }catch (Exception e){
            log.error("Ошибка при обработке запроса на оплату");
        }
    }
}
