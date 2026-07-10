package org.example.exception;


import org.example.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;

/**
 * Централизованный контроллер перехвата исключений биллингового домена.
 * Транслирует финансовые и конкурентные ошибки СУБД в стандартизированные ответы API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обработка запросов к несуществующим финансовым аккаунтам.
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerAccountNotFound(AccountNotFoundException exception){
        return buildResponse("ACCOUNT_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Обработка конфликтов при попытке повторного создания уже существующего счета.
     */
    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlerAccountExists(AccountAlreadyExistsException exception){
        return buildResponse("ACCOUNT_ALLREADY_EXISTS", exception.getMessage(), HttpStatus.CONFLICT);
    }

    /**
     * Обработка невалидных сумм пополнения (меньше или равных нулю).
     */
    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidAmountException(InvalidAmountException exception){
        return buildResponse("INVALID_AMOUNT", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Контроль сквозных заголовков авторизации на стороне биллинга.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handlerMissingHeader(MissingRequestHeaderException exception){
        if ("X-User-Id".equals(exception.getHeaderName())){
            return buildResponse("MISSING_X_USER_ID", "Header X-User_id NOT FOUND", HttpStatus.BAD_REQUEST);
        }
        if ("user_id".equals(exception.getParameter().getParameterName())){
            return buildResponse("MISSING_USER_ID", "Parameter User_id NOT FOUND", HttpStatus.BAD_REQUEST);
        }
        return buildResponse("INTERNAL_ERROR", "Unknown header error", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    /**
     * Обработка конфликтов конкурентного доступа (Optimistic Lock Failure).
     * Срабатывает, если два потока параллельно попытались изменить баланс одного аккаунта.
     * Возвращает клиенту рекомендацию повторить запрос (Retry pattern).
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlerLockingFailure(ObjectOptimisticLockingFailureException exception){
        return buildResponse("CONCURRENT_MODIFICATION", "Please retry the operation", HttpStatus.CONFLICT);
    }

    /**
     * Внутренний фабричный метод сборки иммутабельной обертки ответа об ошибке.
     */
    private ResponseEntity<ErrorResponse> buildResponse(String errorCode, String message, HttpStatus status) {
        ErrorResponse errorDto = new ErrorResponse(errorCode, message, Instant.now());
        return ResponseEntity.status(status).body(errorDto);
    }
}
