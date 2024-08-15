package com.luluroute.ms.service.exception;

public class ShipmentPersistenceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShipmentPersistenceException() {
        super();
    }


    public ShipmentPersistenceException(String message) {
        super(message);
    }

    public ShipmentPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShipmentPersistenceException(String message,
                                        Throwable cause,
                                        boolean enableSuppression,
                                        boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}
