package org.example.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Сущность паттерна Transactional Outbox. Хранит ивенты во временном буфере реляционной БД
 * для последующей гарантированной публикации планировщиком в брокер сообщений.
 */
@Entity
@Table(name = "outbox_events")
@Setter
@Getter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Идентификатор агрегата  orderId для обеспечения сцепления событий.
     */
    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;

    /**
     * Сериализованный строковый JSONB события.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * Флаг-маркер состояния отправки. Переводится в true только после send от брокера.
     */
    @Column(nullable = false)
    private boolean processed = false;

}
