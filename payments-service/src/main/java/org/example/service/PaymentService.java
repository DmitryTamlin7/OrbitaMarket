package org.example.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AccountBalanceDto;
import org.example.entity.Account;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InvalidAmountException;
import org.example.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final AccountRepository accountRepository;

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

    //TODO: Реализовать метод Payment связывает ордер через кафку и списывает кредитыо





}
