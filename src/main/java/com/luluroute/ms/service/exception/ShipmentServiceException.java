package com.luluroute.ms.service.exception;

public class ShipmentServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ShipmentServiceException() {
        super();
    }


    public ShipmentServiceException(String message) {
        super(message);
    }

    public ShipmentServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public ShipmentServiceException(String message,
                                    Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {

        super(message, cause, enableSuppression, writableStackTrace);
    }
}
