package org.example.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Создаем энтити аккаунт делаем только геттер
 * потому что сеттер на все нам не нужен а "@DATA" делает другие служебные методы
 * такие как hashcode equals которые нам не нужны
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
    private String user_id;

    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long balance = 0L;

    @Version
    private Long version;

    public Account(String user_id, Long balance) {
        this.user_id = user_id;
        this.balance = 0L;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
