package org.example.exception;

import jakarta.persistence.EntityNotFoundException;
import org.example.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Централизованный перехватчик исключений слоя REST-контроллеров.
 * Транслирует внутренние бизнес-исключения и ошибки валидации в унифицированный
 * формат ответов ErrorResponse со строгими HTTP-статусами.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Перехват ошибок некорректного заполнения полиморфных геометрических параметров (AOI).
     */
    @ExceptionHandler(InvalidPayloadException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidPayload(InvalidPayloadException exception){
        return buildResponse("INVALID_PAYLOAD", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Отклонение транзакций с отрицательной, нулевой или невалидной ценой.
     */
    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidPrice(InvalidPriceException exception) {
        return buildResponse("INVALID_PRICE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка запросов с неизвестными или неподдерживаемыми спецификациями продуктов.
     */
    @ExceptionHandler(UnknownProductTypeException.class)
    public ResponseEntity<ErrorResponse> handlerUnknownProductType(UnknownProductTypeException exception) {
        return buildResponse("UNKNOWN_PRODUCT_TYPE", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Обработка запросов на получение статуса несуществующего UUID заказа.
     */
    @ExceptionHandler({OrderNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handlerOrderNotFound(Exception exception){
        return buildResponse("ORDER_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    /**
     * Контроль сквозной авторизации. Перехватывает отсутствие обязательных метаданных,
     * генерируя строго регламентированный API Gateway контракт ошибки "Header X-User_id NOT FOUND".
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
     * Внутренний фабричный метод сборки иммутабельной обертки ответа об ошибке.
     */
    private ResponseEntity<ErrorResponse> buildResponse(String errorCode, String message, HttpStatus status){
        ErrorResponse response = new ErrorResponse(errorCode, message, Instant.now());
        return ResponseEntity.status(status).body(response);
    }

}
