package org.example.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaBrokerClient implements MessageBrokerClient{
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, String event) {
        try {
            kafkaTemplate.send(topic, event).get();
            log.info("Событие отправлено в топик: " + topic);
        }catch (Exception e){
            log.warn("Ошибка отправки сообщения в Kafka топик: " + topic + " " + e.getMessage());
        }
    }

    public void send(String topic, Object payload) {
        kafkaTemplate.send(topic, payload);
    }
}
