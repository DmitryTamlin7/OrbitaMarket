package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.example.component.KafkaBrokerClient;
import org.example.dto.*;
import org.example.entity.Account;
import org.example.exception.AccountNotFoundException;
import org.example.exception.InvalidAmountException;
import org.example.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Юнит-тесты для PaymentService")
class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KafkaBrokerClient kafkaBrokerClient;

    @InjectMocks
    private PaymentService paymentService;

    private final String USER_ID = "Dima-01";
    private Account existingAccount;

    @BeforeEach
    void setUp() {
        existingAccount = new Account(USER_ID);
        existingAccount.setBalance(100L);
    }

    @Nested
    @DisplayName("Метод: createAccount")
    class CreateAccountTests {

        @Test
        @DisplayName("Аккаунт уже существует — вернуть текущий баланс")
        void createAccount_WhenAccountExists_ReturnsExisting() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingAccount));

            AccountBalanceDto result = paymentService.createAccount(USER_ID);

            assertNotNull(result);
            assertEquals(USER_ID, result.userId());
            assertEquals(100L, result.balance());
            assertEquals("geocredits", result.currency());
            verify(accountRepository, never()).save(any(Account.class));
        }

        @Test
        @DisplayName("Аккаунта нет — успешно создать новый с дефолтным балансом")
        void createAccount_WhenAccountDoesNotExist_CreatesNew() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            Account newAccount = new Account(USER_ID);
            newAccount.setBalance(0L);
            when(accountRepository.save(any(Account.class))).thenReturn(newAccount);

            AccountBalanceDto result = paymentService.createAccount(USER_ID);

            assertNotNull(result);
            assertEquals(USER_ID, result.userId());
            assertEquals(0L, result.balance());
            verify(accountRepository, times(1)).save(any(Account.class));
        }
    }

    @Nested
    @DisplayName("Метод: getBalance")
    class GetBalanceTests {

        @Test
        @DisplayName("Аккаунт найден — вернуть баланс")
        void getBalance_Success() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingAccount));

            AccountBalanceDto result = paymentService.getBalance(USER_ID);

            assertEquals(100L, result.balance());
        }

        @Test
        @DisplayName("Аккаунт не найден — выбросить AccountNotFoundException")
        void getBalance_NotFound_ThrowsException() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () ->
                    paymentService.getBalance(USER_ID)
            );
            assertEquals("Аккаунт не найден для: " + USER_ID, exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Метод: topUp")
    class TopUpTests {

        @Test
        @DisplayName("Успешное пополнение баланса")
        void topUp_Success() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountBalanceDto result = paymentService.topUp(USER_ID, 50L);

            assertEquals(150L, result.balance());
            verify(accountRepository, times(1)).save(existingAccount);
        }

        @Test
        @DisplayName("Передана сумма <= 0 — выбросить InvalidAmountException")
        void topUp_InvalidAmount_ThrowsException() {
            InvalidAmountException exception = assertThrows(InvalidAmountException.class, () ->
                    paymentService.topUp(USER_ID, 0L)
            );
            assertEquals("Сумма должна быть больше нуля", exception.getMessage());
            verifyNoInteractions(accountRepository);
        }

        @Test
        @DisplayName("Аккаунт для пополнения не найден — выбросить AccountNotFoundException")
        void topUp_AccountNotFound_ThrowsException() {
            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(AccountNotFoundException.class, () ->
                    paymentService.topUp(USER_ID, 50L)
            );
        }
    }

    @Nested
    @DisplayName("Метод: processPayment (Kafka Events)")
    class ProcessPaymentTests {

        private UUID orderId;

        @BeforeEach
        void initId() {
            orderId = UUID.randomUUID();
        }

        @Test
        @DisplayName("Достаточно средств — списать баланс и отправить OrderPaymentCompletedEvent")
        void processPayment_Success() {

            OrderPaymentRequestedEvent mockEvent = mock(OrderPaymentRequestedEvent.class);
            when(mockEvent.orderId()).thenReturn(orderId);
            when(mockEvent.userId()).thenReturn(USER_ID);


            doReturn(40).when(mockEvent).amount();

            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingAccount));


            paymentService.processPayment(mockEvent);


            assertEquals(60L, (long) existingAccount.getBalance());
            verify(accountRepository, times(1)).save(existingAccount);

            ArgumentCaptor<OrderPaymentCompletedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaymentCompletedEvent.class);
            verify(kafkaBrokerClient).send(eq("payment-completed-events"), eventCaptor.capture());

            OrderPaymentCompletedEvent sentEvent = eventCaptor.getValue();
            assertEquals(orderId, sentEvent.orderId());
            assertEquals(USER_ID, sentEvent.userId());
            assertEquals(40, Number.class.cast(sentEvent.amount()).intValue());
        }

        @Test
        @DisplayName("Недостаточно средств — не изменять баланс и отправить OrderPaymentFailedEvent")
        void processPayment_InsufficientBalance() {

            OrderPaymentRequestedEvent mockExpensiveEvent = mock(OrderPaymentRequestedEvent.class);
            when(mockExpensiveEvent.orderId()).thenReturn(orderId);
            when(mockExpensiveEvent.userId()).thenReturn(USER_ID);


            doReturn(150).when(mockExpensiveEvent).amount();

            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingAccount));


            paymentService.processPayment(mockExpensiveEvent);


            assertEquals(100L, (long) existingAccount.getBalance());
            verify(accountRepository, never()).save(any(Account.class));

            ArgumentCaptor<OrderPaymentFailedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPaymentFailedEvent.class);
            verify(kafkaBrokerClient).send(eq("payment-failed-events"), eventCaptor.capture());

            OrderPaymentFailedEvent sentEvent = eventCaptor.getValue();
            assertEquals(orderId, sentEvent.orderId());
            assertEquals(USER_ID, sentEvent.userId());
            assertEquals(FailureReason.INSUFFICIENT_BALANCE, sentEvent.reason());
        }

        @Test
        @DisplayName("Пользователь не найден при обработке ивента — выбросить EntityNotFoundException")
        void processPayment_UserNotFound_ThrowsException() {
            OrderPaymentRequestedEvent mockEvent = mock(OrderPaymentRequestedEvent.class);
            when(mockEvent.userId()).thenReturn(USER_ID);

            when(accountRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () ->
                    paymentService.processPayment(mockEvent)
            );
            verifyNoInteractions(kafkaBrokerClient);
        }
    }
}