package com.luluroute.ms.service.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.luluroute.ms.service.util.ServiceStatusCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ErrorResponse {

    private int statusCode;
    private ServiceStatusCode status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private LocalDateTime timestamp;
    private String message;

    private ErrorResponse() {
        timestamp = LocalDateTime.now();
    }

    ErrorResponse(ServiceStatusCode status) {
        this();
        this.status = status;
    }

    ErrorResponse(ServiceStatusCode status, Throwable ex) {
        this();
        this.status = status;
        this.message = "Unexpected error";
    }

    ErrorResponse(ServiceStatusCode status, String message, Throwable ex) {
        this();
        this.status = status;
        this.message = message;
    }
}
