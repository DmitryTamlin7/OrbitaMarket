package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.component.KafkaBrokerClient;
import org.example.dto.*;
import org.example.entity.Account;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InvalidAmountException;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final KafkaBrokerClient kafkaBrokerClient;

    private final String TOPIC_COMPLETE = "payment-completed-events";
    private final String TOPIC_FAILED = "payment-failed-events";

    public AccountBalanceDto createAccount(String userId){
        Optional<Account> existAccount = accountRepository.findByUserId(userId);
        if (existAccount.isPresent()){
            log.info("Аккаунт есть выдаем его");
            Account account = existAccount.get();
            return new AccountBalanceDto(account.getUserId(), account.getBalance(), "geocredits" );
        }
        log.warn("Аккаунта нет, Создаем...");
        Account newAccount = new Account(userId);
        Account savedAccount = accountRepository.save(newAccount);
        log.info("Аккаунта Сохранен");
        return new AccountBalanceDto(userId, savedAccount.getBalance(), "geocredits");
    }

    public AccountBalanceDto getBalance(String userId) {
        Account existAccount = accountRepository.findByUserId(userId)
                .orElseThrow( () -> new AccountNotFoundException("Аккаунт не найден для: " + userId ));
        log.info("Аккаунт найден");
        return new AccountBalanceDto(existAccount.getUserId(), existAccount.getBalance(), "geocredits");

    }

    @Transactional
    public AccountBalanceDto topUp(String userId, Long amount){
        if (amount <= 0){
            throw new InvalidAmountException("Сумма должна быть больше нуля");
        }
        Account existAccount = accountRepository.findByUserId(userId)
                .orElseThrow( () -> new AccountNotFoundException("Аккаунт не найден для: " + userId ));
        log.info("Аккаунт найден");

        existAccount.setBalance(existAccount.getBalance() + amount);
        Account newBalance = accountRepository.save(existAccount);
        log.info("Баланс увеличен и сохранен");
        return new AccountBalanceDto(newBalance.getUserId(), newBalance.getBalance(), "geocredits");
    }

    @Transactional
    public void processPayment(OrderPaymentRequestedEvent event){
        log.info("Обработка платежа из заказа: {}, пользователя: {}", event.orderId(), event.userId());

        Account account = accountRepository.findByUserId(event.userId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));

        if (account.getBalance() >= event.amount()) {
            account.setBalance(account.getBalance() - event.amount());
            accountRepository.save(account);
            OrderPaymentCompletedEvent successEvent = new OrderPaymentCompletedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    event.userId(),
                    event.amount(),
                    account.getBalance().intValue()
            );
            kafkaBrokerClient.send(TOPIC_COMPLETE, successEvent);
            log.info("Платеж одобрен и выполнен! Списано {}, Баланс: {}", event.amount(), account.getBalance());
        }
        else {
            OrderPaymentFailedEvent failedEvent = new OrderPaymentFailedEvent(
                    UUID.randomUUID(),
                    event.orderId(),
                    event.userId(),
                    FailureReason.INSUFFICIENT_BALANCE
            );
            kafkaBrokerClient.send(TOPIC_FAILED, failedEvent);
            log.error("Платеж отклонен. Недостаточно средств. Баланс: {}  Стоимость: {}", account.getBalance(), event.amount());
        }
    }
}
