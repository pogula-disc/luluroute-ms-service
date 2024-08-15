package com.luluroute.ms.service.exception;

public class DataSerializationException extends Exception {
    public DataSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSerializationException(String message) {
        super(message);
    }
}