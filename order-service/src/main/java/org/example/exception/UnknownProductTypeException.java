package org.example.exception;

public class UnknownProductTypeException extends RuntimeException {
    public UnknownProductTypeException(String message) {
        super(message);
    }
}
