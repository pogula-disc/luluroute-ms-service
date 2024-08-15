package com.luluroute.ms.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.OriginInfo;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.dto.ServiceCancelDto;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.ShipmentCancellationException;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.exception.SystemDownException;
import com.luluroute.ms.service.redis.ShipmentMessageGlobalCache;
import com.luluroute.ms.service.service.SvcMessageService;
import com.luluroute.ms.service.service.impl.BaseShipmentService;
import com.luluroute.ms.service.transformer.ShipmentTransformerService;
import com.luluroute.ms.service.util.ObjectMapperUtil;
import com.luluroute.ms.service.util.RequestType;
import com.luluroute.ms.service.util.ShipmentCorrelationIdUtil;
import com.luluroute.ms.service.validator.ShipmentValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.luluroute.ms.service.util.MultiCarrierAttributesUtil.removeMultiCarrierAttributes;
import static com.luluroute.ms.service.util.ShipmentConstants.*;
import static org.slf4j.MDC.getCopyOfContextMap;

@Service
@Slf4j
public class ShipmentMessageProcessor extends BaseShipmentService {

    @Autowired
    private KafkaShipmentProducer kafkaShipmentProducer;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private SvcMessageService svcMessageService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    ShipmentMessageGlobalCache shipmentMessageGlobalCache;
    @Autowired
    @Qualifier("SvcMessageExecutor")
    Executor svcMessageExecutor;
    @Autowired
    Executor svcDBExecutor;
    @Autowired
    private ShipmentTransformerService shipmentTransformerService;

    public ShipmentMessageProcessor(AppConfig appConfig, ObjectMapper objectMapper) {
        super(appConfig, objectMapper);
    }

    public CompletableFuture<ShipmentServiceResponse> processShipmentMessage(ShipmentMessage shipmentMessage) throws SystemDownException {
        long start = System.currentTimeMillis();
        String shipmentCorrelationId = "";
        // A Future chain for related DB transactions, in order
        CompletableFuture<Void> svcDBExecution = CompletableFuture.runAsync(() -> { }, svcDBExecutor);
        try {
            log.info("Request processing through 2.0 - request: {}", getJsonString(shipmentMessage));
            // Verify and populate ShipmentCorrelationId for ShipmentStatus-'NEW'
            shipmentCorrelationId = populateShipmentCorrelationId(shipmentMessage);
            MDC.put(X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);

            boolean isCancelRequest = isCancelRequest(shipmentMessage);
            MDC.put(CANCEL_CONTEXT, Boolean.toString(isCancelRequest));
            if(!isCancelRequest) {
                svcDBExecution = svcMessageService.saveShipmentMessage(shipmentMessage, shipmentCorrelationId, svcDBExecution);
            }

            // Run Validator to validate any attribute in request
            ShipmentValidator validator = this.getShipmentValidator(shipmentMessage);

            if (!validator.isPayloadValid(shipmentMessage))
                return handleInvalidPayload(shipmentMessage, shipmentCorrelationId, svcDBExecution);
            if(!appConfig.isEnableMultiCarrierAttributesFor2_0())
                removeMultiCarrierAttributes(shipmentMessage);

            log.info("Request processing > validations completed}");

            if (hasPerformanceAttributes(shipmentMessage))
                return handleMockedResponse(start);

            // Run Transformer to update any attribute based on meta data
            shipmentTransformerService.transform(shipmentMessage);

            log.info("Request processing > transformation completed}");

            // persist the correlationId to Redis for processing
            shipmentMessageGlobalCache.save(shipmentCorrelationId, shipmentMessage);

            // publish the message to Integration_Master topic
            publishShipmentMessage(shipmentMessage, shipmentCorrelationId, shipmentMessage.getRequestHeader().getRequestType());

            if (isCancelRequest) {
                return processCancelShipmentRequest(shipmentCorrelationId, shipmentMessage, svcDBExecution);
            } else {
                return processCreateShipmentRequest(shipmentCorrelationId, shipmentMessage, svcDBExecution);
            }
        } catch (ShipmentCancellationException sce) {
            throw sce;
        } catch (Exception exception) {
            log.error("Luluroute 2.0 | Error Occurred |  shipmentCorrelationId: {}, Error: {}",
                    shipmentCorrelationId, ExceptionUtils.getStackTrace(exception));
            svcMessageService.updateShipmentCreateFailure(null, shipmentCorrelationId, svcDBExecution);

            throw new ShipmentServiceException("Luluroute 2.0 | Error Occurred | "
                    + exception.getMessage());
        } finally {
            MDC.remove(X_TRANSACTION_REFERENCE);
        }
    }

    private CompletableFuture<ShipmentServiceResponse> processCreateShipmentRequest(
            String shipmentCorrelationId, ShipmentMessage shipmentMessage, CompletableFuture<Void> svcDbExecution) {
        log.info("Processing a non-cancel shipment request for ShipmentCorrelationId # {}. Saving shipment message.", shipmentCorrelationId);
        Map<String, String> mdcContext = getCopyOfContextMap();
        return getShipmentResponseMessage(shipmentCorrelationId, mdcContext)
                .thenApply(responseMessage -> buildShipmentResponse(
                        responseMessage, shipmentCorrelationId, shipmentMessage, svcDbExecution));
    }

    private CompletableFuture<ShipmentServiceResponse> processCancelShipmentRequest(
            String shipmentCorrelationId, ShipmentMessage shipmentMessage, CompletableFuture<Void> svcDbExecution) {
        log.info("Processing cancel request for ShipmentCorrelationId # {}", shipmentCorrelationId);
        String shipmentStatus = svcMessageService.getCanceledShipment(shipmentCorrelationId);
        if (SHIPMENT_CANCELED_TEXT.equals(shipmentStatus)) {
            throw new ShipmentCancellationException(CANCEL_RESPONSE_ALREADY_CANCELLED, shipmentMessage);
        } else if (SHIPMENT_NOT_FOUND.equals(shipmentStatus)) {
            throw new ShipmentCancellationException(CANCEL_RESPONSE_SHIPMENT_NOT_FOUND, shipmentMessage);
        }
        return getCancelShipmentResponseMessage(shipmentCorrelationId, shipmentMessage)
                .thenApply(responseMessage -> buildShipmentResponse(
                        responseMessage, shipmentCorrelationId, shipmentMessage, svcDbExecution));
    }

    private ShipmentServiceResponse buildShipmentResponse(
            ShipmentMessage responseMessage, String shipmentCorrelationId, ShipmentMessage originalMessage,
            CompletableFuture<Void> svcDbExecution) {
        MDC.put(X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);
        if(responseMessage != null) {
            buildSuccessResponse(responseMessage, shipmentCorrelationId, originalMessage, svcDbExecution);
        } else {
            buildFailureResponse(originalMessage, shipmentCorrelationId, svcDbExecution);
        }
        MDC.remove(X_SHIPMENT_CORRELATION_ID);
        return buildShipmentServiceResponse(responseMessage, originalMessage);
    }

    private void buildSuccessResponse(
            ShipmentMessage responseMessage, String shipmentCorrelationId, ShipmentMessage originalMessage,
            CompletableFuture<Void> svcDbExecution) {
        shipmentMessageGlobalCache.removeShipmentMessage(shipmentCorrelationId);

        if (Objects.nonNull(responseMessage.getRequestHeader().getResponse())) {
            responseMessage.getRequestHeader().getResponse().setResponseDate(Instant.now().getEpochSecond());
            if (SUCCESS_RESPONSE.equalsIgnoreCase(responseMessage.getRequestHeader().getResponse().getResponseCode())) {
                log.info("Request processed successfully through 2.0 ShipmentCorrelationId # {} response # {}",
                        shipmentCorrelationId, originalMessage);
                svcMessageService.updateShipmentMessage(responseMessage, shipmentCorrelationId, svcDbExecution);
            } else {
                log.info("Request processed with errors through 2.0 ShipmentCorrelationId # {} response # {}",
                        shipmentCorrelationId, originalMessage);
                svcMessageService.updateShipmentCreateFailure(responseMessage, shipmentCorrelationId, svcDbExecution);
            }
        }
        Optional.ofNullable(responseMessage.getMessageHeader()).ifPresent(messageHeaderInfo ->
                messageHeaderInfo.setMessageSources(originalMessage.getMessageHeader().getMessageSources()));
    }

    private void buildFailureResponse(
            ShipmentMessage originalMessage, String shipmentCorrelationId, CompletableFuture<Void> svcDbExecution) {
        ResponseItem responseItem = ResponseItem.builder()
                .responseCode(FAILURE_RESPONSE)
                .responseDate(Instant.now().getEpochSecond())
                .extended(List.of(ItemInfo.builder().value("Unable to process ShipmentMessage").build()))
                .build();
        originalMessage.getRequestHeader().setResponse(responseItem);
        svcMessageService.updateShipmentCreateFailure(originalMessage, shipmentCorrelationId, svcDbExecution);
        log.info("Request processed with exceptions/errors through 2.0 ShipmentCorrelationId # {} response # {}",
                shipmentCorrelationId, originalMessage);
    }

    private ShipmentServiceResponse buildShipmentServiceResponse(ShipmentMessage responseMsg, ShipmentMessage originalMessage) {
        if (responseMsg != null) {
            return ShipmentServiceResponse.builder()
                    .success(true)
                    .shipmentMessage(responseMsg)
                    .message(RESPONSE_PERSISTED_SUCCESSFULLY)
                    .build();
        } else {
            return ShipmentServiceResponse.builder()
                    .shipmentMessage(originalMessage)
                    .message("Failed to process shipment message.")
                    .success(false)
                    .build();
        }
    }

    private CompletableFuture<ShipmentServiceResponse> handleMockedResponse(long start) {
        try {
            ShipmentMessage respShipmentMsg = getMockedShipmentMessage();
            log.debug("Time taken for mocked response: {} ms", System.currentTimeMillis() - start);
            return CompletableFuture.completedFuture(ShipmentServiceResponse.builder().success(true)
                    .shipmentMessage(respShipmentMsg)
                    .message("Shipment message is sent to kafka topic").build());
        } catch (IOException ioException) {
            log.error("IOException occurred while getting mocked ShipmentMessage.");
            throw new ShipmentServiceException(ioException.getMessage(), ioException);
        }
    }

    private CompletableFuture<ShipmentServiceResponse> handleInvalidPayload(
            ShipmentMessage shipmentMessage, String shipmentCorrelationId, CompletableFuture<Void> svcDbExecution) {
        ShipmentServiceResponse response = ShipmentServiceResponse.builder()
                .success(false)
                .message("Shipment payload is invalid.")
                .shipmentMessage(shipmentMessage)
                .build();
        log.info("Responding with validation errors: {}", response);
        svcMessageService.updateShipmentCreateValidationFailure(shipmentMessage, shipmentCorrelationId, svcDbExecution);
        return CompletableFuture.completedFuture(response);
    }

    private String populateShipmentCorrelationId(ShipmentMessage shipmentMessage) {
        ShipmentCorrelationIdUtil.populateShipmentCorrelationId(shipmentMessage);
        return shipmentMessage.getMessageBody().getShipments().stream()
                .map(shipmentInfo -> shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                .collect(Collectors.joining());
    }

    private CompletableFuture<ShipmentMessage> getCancelShipmentResponseMessage(String shipmentCorrelationId, ShipmentMessage shipmentMessage) {
        return CompletableFuture.supplyAsync(() -> {
            MDC.put(X_SHIPMENT_CORRELATION_ID, shipmentCorrelationId);
            log.info("Request processing cancel response");

            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;

            ShipmentMessage responseMsg = shipmentMessageGlobalCache.getShipmentCancelResponse(shipmentCorrelationId);

            // Check the cache for the response message periodically until the message is found
            while (responseMsg == null && elapsedTime < appConfig.getResponseTimeoutMillis()) {
                try {
                    // Sleep for 1 second before checking again
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Handle interruption if needed
                    Thread.currentThread().interrupt();
                    log.error("Luluroute 2.0 | Error Occurred | Request processing cancel response correlationId: {}", shipmentCorrelationId, e);
                    throw new ShipmentServiceException("Cancel Shipment interrupted while waiting for response.", e);
                }

                elapsedTime = System.currentTimeMillis() - startTime;
                responseMsg = shipmentMessageGlobalCache.getShipmentCancelResponse(shipmentCorrelationId);

                // Break the loop if the response message is found
                if (responseMsg != null) {
                    break;
                }
            }

            if (responseMsg == null) {
                log.error("Luluroute 2.0 | Timeout occurred | Request processing cancel response : {}", shipmentCorrelationId);
                throw new ShipmentCancellationException(CANCEL_RESPONSE_TIMEOUT, shipmentMessage);
            } else {
                log.info("Time taken for shipment with correlationId {}: {} milliseconds", shipmentCorrelationId, elapsedTime);
            }

            log.info("Request processing cancel response received");
            MDC.remove(X_SHIPMENT_CORRELATION_ID);

            return ObjectMapperUtil.map(responseMsg, ShipmentMessage.class);
        }, svcMessageExecutor);
    }

    private static ServiceCancelDto getServiceCancelDto(ShipmentMessage shipmentMessage) {
        return ServiceCancelDto.builder().svcCancelId(UUID.randomUUID())
                .svcRequestDate(new Date(shipmentMessage.getRequestHeader().getRequestDate()))
                .svcRequestType(shipmentMessage.getRequestHeader().getRequestType())
                .messageStatus(shipmentMessage.getMessageStatus().getStatus())
                .messageCorrelationId(shipmentMessage.getMessageHeader().getMessageCorrelationId())
                .sequence(shipmentMessage.getMessageHeader().getSequence())
                .totalSequence(shipmentMessage.getMessageHeader().getTotalSequence())
                .messageStatusDate(new Date(shipmentMessage.getMessageHeader().getMessageDate()))
                .roleType(shipmentMessage.getMessageHeader().getMessageSources().get(0).getRoleType())
                .entityCode(shipmentMessage.getMessageHeader().getMessageSources().get(0).getEntityCode())
                .shipmentCorrelationId(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getShipmentCorrelationId())
                .shipmentStatus(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentStatus().getStatus())
                .shipmentStatusDate(new Date(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentStatus().getStatusDate())).build();
    }

    private void publishShipmentMessage(ShipmentMessage shipmentReq, String shipmentCorrelationId, String requestType) {
        com.logistics.luluroute.avro.shipment.message.ShipmentMessage avroShipment = ObjectMapperUtil.map(shipmentReq,
                com.logistics.luluroute.avro.shipment.message.ShipmentMessage.class);

        kafkaShipmentProducer.publishMessage(avroShipment, shipmentCorrelationId,
                this.getMessageEntityCode(shipmentReq), this.getOriginEntityCode(shipmentReq),
                RequestType.CANCEL_REQ.getValue().equals(requestType)
                        ? svcMessageService.getCarrierDetails(shipmentCorrelationId)
                        : "");
    }

    public CompletableFuture<ShipmentMessage> getShipmentResponseMessage(String shipmentCorrelationId, Map<String, String> mdcContext) {
        return CompletableFuture.supplyAsync(() -> {
            if(mdcContext != null) {
                MDC.setContextMap(mdcContext);
            }
            try {
                log.info("Request processing label response");

                long startTime = System.currentTimeMillis();
                long elapsedTime = 0;

                ShipmentMessage responseMsg = shipmentMessageGlobalCache.getShipmentResponse(shipmentCorrelationId);

                // Check the cache for the response message periodically until the message is found
                while (responseMsg == null && elapsedTime < appConfig.getResponseTimeoutMillis()) {
                    try {
                        // Sleep for 1 second before checking again
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // Handle interruption if needed
                        Thread.currentThread().interrupt();
                        log.error("Luluroute 2.0 | Error Occurred |Request processing label response  correlationId: {}", shipmentCorrelationId, e);
                        throw new ShipmentServiceException("Shipment service was interrupted while waiting for response.", e);
                    }

                    elapsedTime = System.currentTimeMillis() - startTime;
                    responseMsg = shipmentMessageGlobalCache.getShipmentResponse(shipmentCorrelationId);

                    // Break the loop if the response message is found
                    if (responseMsg != null) {
                        break;
                    }
                }

                if (responseMsg == null) {
                    log.error("Luluroute 2.0 | Timeout occurred |Request processing label response correlationId: {}", shipmentCorrelationId);
                    throw new ShipmentServiceException("Timeout occurred while waiting for shipment response");
                } else {
                    log.info("Time taken for shipment with correlationId {}: {} milliseconds", shipmentCorrelationId, elapsedTime);
                }

                log.info("Request processing label response completed");
                return ObjectMapperUtil.map(responseMsg, ShipmentMessage.class);
            } finally {
                MDC.clear();
            }

        }, svcMessageExecutor);
    }

    public String getMessageEntityCode(@NotNull ShipmentMessage shipmentMessage) {
        log.debug("ShipmentProducerService - MessageEntityCode");
        String msgEntityCode = shipmentMessage.getMessageHeader().getMessageSources()
                .stream().map(EntityRole::getEntityCode).collect(Collectors.joining(KEY_DELIMITER));
        log.debug("ShipmentProducerService - MessageEntityCode: {}", msgEntityCode);
        return msgEntityCode;
    }

    public String getOriginEntityCode(@NotNull ShipmentMessage shipmentMessage) {
        log.debug("ShipmentProducerService - OriginEntityCode");
        String entityCode = shipmentMessage.getMessageBody().getShipments()
                .stream().map(shipmentInfo -> shipmentInfo.getShipmentHeader()
                        .getOrigin()).filter(Objects::nonNull).map(OriginInfo::getEntityCode)
                .collect(Collectors.joining(KEY_DELIMITER));
        log.debug("ShipmentProducerService - OriginEntityCode: {}", entityCode);
        return entityCode;
    }

    protected String getJsonString(ShipmentMessage shipmentMessage) throws JsonProcessingException {
        objectMapper.writerWithDefaultPrettyPrinter();
        return objectMapper.writeValueAsString(shipmentMessage);
    }

    private boolean isCancelRequest(ShipmentMessage shipmentMessage) {
        if(shipmentMessage != null && shipmentMessage.getRequestHeader() != null) {
            return RequestType.CANCEL_REQ.getValue().equals(shipmentMessage.getRequestHeader().getRequestType());
        }
        return false;
    }
}
