package org.example.exception;


import org.example.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.security.auth.login.AccountNotFoundException;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlerAccountNotFound(AccountNotFoundException exception){
        return buildResponse("ACCOUNT_NOT_FOUND", exception.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlerAccountExists(AccountAlreadyExistsException exception){
        return buildResponse("ACCOUNT_ALLREADY_EXISTS", exception.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ResponseEntity<ErrorResponse> handlerInvalidAmountException(InvalidAmountException exception){
        return buildResponse("INVALID_AMOUNT", exception.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handlerMissingHeader(MissingRequestHeaderException exception){
        if ("X-User-Id".equals(exception.getHeaderName())){
            return buildResponse("MISSION_USER_ID", "Header X-User_id NOt FOUND", HttpStatus.BAD_REQUEST);
        }
        return buildResponse("INTERNAL_ERROR", "Unknown header error", HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlerLockingFailure(ObjectOptimisticLockingFailureException exception){
        return buildResponse("CONCURRENT_MODIFICATION", "Pleace retry the operation", HttpStatus.CONFLICT);
    }


    private ResponseEntity<ErrorResponse> buildResponse(String errorCode, String message, HttpStatus status) {
        ErrorResponse errorDto = new ErrorResponse(errorCode, message, Instant.now());
        return ResponseEntity.status(status).body(errorDto);
    }



}
