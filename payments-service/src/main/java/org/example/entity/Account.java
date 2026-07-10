package org.example.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

/**
 * сущность финансового счета пользователя в системе OrbitaMarket.
 * Инкапсулирует баланс расчетных единиц (geocredits).
 *  Использование аннотации @Data намеренно избегается для предотвращения автоматической
 * генерации неоптимальных методов hashCode/equals и избыточных мутаторов (сеттеров),
 * что гарантирует контролируемое изменение состояния счета только через выделенные методы.
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long balance = 0L;

    /**
     * Версия сущности для реализации механизма оптимистичной блокировки (Optimistic Locking).
     * Защищает баланс от состояния гонки (Race Condition) при одновременных запросах на списание/пополнение.
     */
    @Version
    private Long version;

    /**
     * Конструктор первичной инициализации счета пользователя.
     */
    public Account(String userId) {
        this.userId = userId;
        this.balance = 0L;
    }

    /**
     * Явный сеттер для контролируемого изменения баланса бизнес-логикой процессора.
     */
    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
