package org.example.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jdk.jfr.Description;
import lombok.RequiredArgsConstructor;
import org.example.dto.AccountBalanceDto;
import org.example.dto.ErrorResponse;
import org.example.dto.TopUpRequest;
import org.example.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/payments/accounts")
@RequiredArgsConstructor
@Description("Управление счетами и балансом геокредитов")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Создание счета", description = "Идемпотентное создание счета для пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счет успешно создан или уже существует"),
            @ApiResponse(responseCode = "400", description = "Отсутствует заголовок X-User-Id",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccountBalanceDto> createAccount(
            @Parameter(description = "ID пользователя", required = true)
            @RequestHeader("X-User-Id") String userId){

        AccountBalanceDto account = paymentService.createAccount(userId);
        return ResponseEntity.ok(account);
    }


    @PostMapping("/top-up")
    @Operation(summary = "Пополнение баланса", description = "Начисление геокредитов на счет пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс успешно пополнен"),
            @ApiResponse(responseCode = "400", description = "Неверная сумма (<= 0) или нет заголовка",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Счет не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Конфликт параллельного пополнения (Optimistic Lock)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccountBalanceDto> topUP(
            @Parameter(description = "ID пользоватлея", required = true)
            @RequestHeader("X-User-Id") String userId,
            @RequestBody TopUpRequest request) {
        AccountBalanceDto updatedAcc = paymentService.topUp(userId, request.amount());
        return ResponseEntity.ok(updatedAcc);
    }

    @GetMapping("/balance")
    @Operation(summary = "Получение баланса", description = "Возвращает текущий баланс геокредитов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Баланс успешно получен"),
            @ApiResponse(responseCode = "404", description = "Счет не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccountBalanceDto> getBalance(
        @Parameter(description = "ID пользоватлея", required = true)
        @RequestHeader("X-User-Id") String userId){

            AccountBalanceDto balance = paymentService.getBalance(userId);
            return ResponseEntity.ok(balance);
    }
}

