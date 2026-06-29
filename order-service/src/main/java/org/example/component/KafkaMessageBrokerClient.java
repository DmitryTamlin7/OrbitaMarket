package org.example.component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class KafkaMessageBrokerClient implements  MessageBrokerClient{
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void send(String topic, String payload) {
        try {
            kafkaTemplate.send(topic, payload).get();
            log.info("Событие отправлено в топик: " + topic);
        }catch (Exception e){
            log.warn("Ошибка отправки сообщения в Kafka топик: " + topic + " " + e.getMessage());
        }
    }
}
