package org.example.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность-маркер распределенной дедупликации (Паттерн Idempotent Consumer).
 * Хранит идентификаторы успешно обработанных заказов для предотвращения
 * повторных списаний (Double Spending) при дублировании сообщений в Apache Kafka.
 */
@Entity
@Table(name = "processed_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOrder {
    @Id
    private UUID orderId;
    private LocalDateTime processedAt;
}
