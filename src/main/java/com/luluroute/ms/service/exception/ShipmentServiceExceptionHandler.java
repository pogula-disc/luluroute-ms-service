package com.luluroute.ms.service.exception;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.kafka.ShipmentCancelResponseBuilder;
import com.luluroute.ms.service.util.ServiceStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.luluroute.ms.service.util.ShipmentConstants.SUCCESS_RESPONSE;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@Slf4j
public class ShipmentServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ShipmentServiceException.class)
    protected ResponseEntity<Object> handleShipmentServiceException(ShipmentServiceException ex) {
        log.error("An internal error occurred. ShipmentServiceException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidInputException.class)
    protected ResponseEntity<Object> handleInvalidInputException(InvalidInputException ex) {
        log.error("Shipment service - request payload is invalid. InvalidInputException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_400);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_400.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<Object> handleInvalidTokenException(InvalidTokenException ex) {
        log.error("Shipment service - Jwt token is expired or invalid.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_400);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_400.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceAccessException.class)
    protected ResponseEntity<Object> handleResourceAccessException(ResourceAccessException ex) {
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_990);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_990.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    protected ResponseEntity<Object> handleDataAccessException(DataAccessException ex) {
        log.error("An internal error occurred. DataAccessException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ShipmentCancellationException.class)
    protected ResponseEntity<Object> handleShipmentCancellationException(ShipmentCancellationException ex) {
        log.error("Unable to process shipment cancellation. ShipmentCancellationException exception was thrown.", ex);
        ShipmentMessage shipmentMessage = ex.getShipmentMessage();
        String messageCorrelationId = shipmentMessage.getMessageHeader().getMessageCorrelationId();
        String shipmentCorrelationId = shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getShipmentCorrelationId();
        ShipmentMessage cancelShipmentResponse = ShipmentCancelResponseBuilder.buildCancelShipmentResponse(messageCorrelationId, shipmentCorrelationId, ex.getMessage());
        return new ResponseEntity<>(cancelShipmentResponse, HttpStatus.OK);
    }

    @Override
    public @NotNull ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                        @NotNull HttpHeaders headers,
                                                                        HttpStatus status,
                                                                        @NotNull WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("timestamp", Instant.now());

        //Get all errors
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.toList());

        body.put("errors", errors);

        return new ResponseEntity<>(body, headers, status);

    }

    @ExceptionHandler(InterruptedException.class)
    protected ResponseEntity<Object> handleInterruptedException(InterruptedException ex) {
        log.error("An internal error occurred. InterruptedException exception was thrown.", ex);
        ErrorResponse errorResponse = new ErrorResponse(ServiceStatusCode.SVC_ERROR_999);
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setStatusCode(ServiceStatusCode.SVC_ERROR_999.value());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
