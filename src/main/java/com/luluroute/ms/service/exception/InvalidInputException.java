package com.luluroute.ms.service.exception;

public class InvalidInputException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidInputException() {
        super();
    }


    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInputException(String message,
                                 Throwable cause,
                                 boolean enableSuppression,
                                 boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}
