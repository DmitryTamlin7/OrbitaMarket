package org.example.component;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.OutboxEvent;
import org.example.repository.OutBoxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * Планировщик публикации событий по паттерну Transactional Outbox
 * Обеспечивает гарантированную доставку сообщений (At-Least-Once Delivery) в брокер Kafka,
 * полностью исключая рассинхронизацию между состоянием БД и шиной данных при падении сети.
 */
@Slf4j
@Component
@AllArgsConstructor
public class OutboxScheduler {
    private static final String ORDER_TOPIC = "order-payment-request";
    private final OutBoxEventRepository outBoxEventRepository;
    private final KafkaMessageBrokerClient brokerClient;

    /**
     * Фоновая задача периодического опроса Outbox-таблицы
     * Вычитывает необработанные события строго в хронологическом порядке (FIFO).
     * При успешной отправке в Kafka отмечает событие как обработанное
     * При сбое сети транзакция откатывается, а цикл прерывается для сохранения строгого порядка
     */
    @Scheduled(fixedDelay = 1500)
    @Transactional
    public void processedOutboxEvent(){
        List<OutboxEvent> outboxEventList = outBoxEventRepository.findByProcessedFalseOrderByCreatedAtAsc();
        if (outboxEventList.isEmpty()){
            log.info("Новых записей нет");
            return;
        }
        log.info("Найдено {} Необработанных событий", outboxEventList.size());

        for (OutboxEvent event : outboxEventList){
            try {
                brokerClient.send(ORDER_TOPIC, event.getPayload());
                event.setProcessed(true);
                outBoxEventRepository.save(event);
            }
            catch (Exception e){
                log.error("Ошибка отправки события в брокер: " + e.getMessage());
                break;
            }
        }
    }
}
