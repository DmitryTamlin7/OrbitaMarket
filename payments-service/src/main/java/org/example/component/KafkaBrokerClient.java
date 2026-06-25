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
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void send(String topic, Object event) {
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, json).get();
            log.info("Событие отправлено в топик: " + topic);
        }catch (Exception e){
            log.warn("Ошибка отправки сообщения в Kafka топик: " + topic + " " + e.getMessage());
        }
    }
}
