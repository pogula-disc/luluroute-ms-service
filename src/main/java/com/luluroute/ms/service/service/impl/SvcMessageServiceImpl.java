package com.luluroute.ms.service.service.impl;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ServiceMessageDto;
import com.luluroute.ms.service.dto.ServiceMessageResponse;
import com.luluroute.ms.service.dto.ShipmentSearchDto;
import com.luluroute.ms.service.model.ServiceMessage;
import com.luluroute.ms.service.repository.ServiceMessageRepository;
import com.luluroute.ms.service.service.SvcMessageService;
import com.luluroute.ms.service.util.ObjectMapperUtil;
import com.luluroute.ms.service.util.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.luluroute.ms.service.util.RequestType.CANCEL_REQ;
import static com.luluroute.ms.service.util.RequestType.SHIPMENT_REQ;
import static com.luluroute.ms.service.util.ShipmentConstants.*;
import static org.slf4j.MDC.getCopyOfContextMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SvcMessageServiceImpl implements SvcMessageService {

    private final ServiceMessageRepository messageRepository;
    private final Executor svcDBExecutor;

    @Override
    public ServiceMessageResponse createServiceMessage(ServiceMessageDto messageDto) {
        log.info("Calling processServiceMessage in SvcMessageServiceImpl");
        ServiceMessage svcMessage = ObjectMapperUtil.map(messageDto, ServiceMessage.class);
        svcMessage.setSvcMessageId(UUID.randomUUID());
        svcMessage.setShipmentCorrelationId(messageDto.getShipmentCorrelationId());
        messageRepository.save(svcMessage);

        log.info("SvcMessage is persisted into DB successfully");
        return ServiceMessageResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }

    @Override
    public ServiceMessageResponse cancelServiceMessage(String shipCorrId) {
        Optional<ServiceMessage> optSvcMsg = Optional.ofNullable(messageRepository
                .findByActiveAndShipmentCorrelationId(1, shipCorrId));
        if (optSvcMsg.isPresent()) {
            ServiceMessage serviceMessage = optSvcMsg.get();
            serviceMessage.setActive(0);
            serviceMessage.setUpdatedBy("User");
            messageRepository.save(serviceMessage);
            log.info("ServiceMessage is deactivated!");
            return ServiceMessageResponse.builder().message("ServiceMessage is deactivated successfully!").build();
        }
        return ServiceMessageResponse.builder()
                .message(String.format("No ServiceMessage is found for ShipmentCorrelationId: %s", shipCorrId))
                .build();
    }

    @Override
    public ServiceMessageResponse searchServiceMessage(ShipmentSearchDto searchDto) {
        Optional<List<ServiceMessage>> optMessages = Optional.ofNullable(
                messageRepository.searchServiceMessage(searchDto.getShipmentCorrelationId(),
                        searchDto.getServiceDate()));
        if (optMessages.isPresent()) {
            return ServiceMessageResponse.builder()
                    .serviceMessages(ObjectMapperUtil.mapAll(optMessages.get(), ServiceMessageDto.class))
                    .message("ServiceMessage(s) found!").build();
        }
        return ServiceMessageResponse.builder().message("No ServiceMessage found").build();
    }

    public CompletableFuture<Void> saveShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution) {
        Map<String, String> mdcContext = getCopyOfContextMap();
        return svcDbExecution.thenRunAsync(() -> saveShipmentMessage(shipmentMessage, shipCorrelationId, mdcContext), svcDBExecutor);
    }


    private void saveShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, Map<String, String> mdcContext) {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        try {
            log.info("ShipmentMessage persistance into DB started");
            ServiceMessage msgEntity = ServiceMessage.builder().message(shipmentMessage).svcMessageId(UUID.randomUUID())
                    .svcMessageDate(new Date()).shipmentCorrelationId(shipCorrelationId)
                    .svcMessageType(Integer.valueOf(shipmentMessage.getRequestHeader().getRequestType()))
                    .active(1).shipmentStatus(SHIPMENT_INITIATED_TEXT).build();
            messageRepository.save(msgEntity);
            log.info("ShipmentMessage is persisted into DB successfully");
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        } finally {
            MDC.clear();
        }
    }

    public void saveLegacyShipmentMessage(String shipmentId, String originEntity, String carrierCode) {
        try {
            if (StringUtils.isNotEmpty(shipmentId)) {

                log.info("Legacy Shipment # {} persistance into DB started.", shipmentId);

                ServiceMessage msgEntity = ServiceMessage.builder().message(null)
                        .svcMessageId(UUID.randomUUID())
                        .svcMessageDate(new Date()).shipmentCorrelationId(shipmentId)
                        .svcMessageType(2001)
                        .originEntity(originEntity)
                        .carrierCode(carrierCode)
                        .active(1)
                        .shipmentStatus(SHIPMENT_CREATED_TEXT).build();
                messageRepository.save(msgEntity);
                log.info("Legacy Shipment # {} is persisted into DB successfully", shipmentId);
            }
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        } finally {
            MDC.clear();
        }
    }

    public CompletableFuture<Void> updateShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution) {
        Map<String, String> mdcContext = getCopyOfContextMap();
        return svcDbExecution.thenRunAsync(() -> updateShipmentMessage(shipmentMessage, shipCorrelationId, mdcContext), svcDBExecutor);
    }

    private void updateShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, Map<String, String> mdcContext) {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        log.info("ShipmentMessage update into DB started");

        try {
            String shipmentMessageStr = new com.fasterxml.jackson.databind
                    .ObjectMapper().writeValueAsString(shipmentMessage);

            if (SHIPMENT_REQ.equals(RequestType.getRequestTypeByValue(shipmentMessage.getRequestHeader().getRequestType()))) {
                String originEntity = getOriginEntityFromShipmentMessage(shipmentMessage);
                String carrierCode = getCarrierCodeFromShipmentMessage(shipmentMessage);
                messageRepository.updateServiceMessageResponse(
                        shipmentMessageStr, SHIPMENT_CREATED_TEXT, originEntity, carrierCode, shipCorrelationId, new Date(), UPDATED_BY);
            } else if (CANCEL_REQ.equals(RequestType.getRequestTypeByValue(shipmentMessage.getRequestHeader().getRequestType()))) {
                messageRepository.updateServiceMessageStatus(SHIPMENT_CANCELED_TEXT, shipCorrelationId, new Date(), UPDATED_BY);
            }

            log.info("ShipmentMessage update into DB successfully");
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        } finally {
            MDC.clear();
        }
    }


    public CompletableFuture<Void> updateShipmentCreateValidationFailure(
            ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution) {
        Map<String, String> mdcContext = getCopyOfContextMap();
        return svcDbExecution.thenRunAsync(() -> updateShipmentCreateFailure(shipmentMessage, shipCorrelationId, mdcContext, true), svcDBExecutor);
    }

    public CompletableFuture<Void> updateShipmentCreateFailure(
            ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution) {
        Map<String, String> mdcContext = getCopyOfContextMap();
        return svcDbExecution.thenRunAsync(() -> updateShipmentCreateFailure(shipmentMessage, shipCorrelationId, mdcContext, false), svcDBExecutor);
    }

    private void updateShipmentCreateFailure(
            ShipmentMessage shipmentMessage, String shipCorrelationId, Map<String, String> mdcContext, boolean isValidation) {
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        }
        // In the case of an exception, shipmentMessage will be null, so don't rely on shipmentMessage for cancel status
        // Do not save failures related to cancel messages
        if (BooleanUtils.toBoolean(MDC.get(CANCEL_CONTEXT)))
            return;

        log.info("ShipmentMessage (with failure response) update into DB started");

        try {
            if (shipmentMessage != null) {
                String shipmentMessageStr = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(shipmentMessage);
                String originEntity = getOriginEntityFromShipmentMessage(shipmentMessage);
                String carrierCode = getCarrierCodeFromShipmentMessage(shipmentMessage);
                String status = isValidation ? SHIPMENT_NOT_VALID_TEXT : SHIPMENT_NOT_CREATED_TEXT;
                messageRepository.updateServiceMessageResponse(
                        shipmentMessageStr, status, originEntity, carrierCode, shipCorrelationId, new Date(), UPDATED_BY);
            } else {
                messageRepository.updateServiceMessageStatus(SHIPMENT_NOT_CREATED_TEXT, shipCorrelationId, new Date(), UPDATED_BY);
            }

            log.info("ShipmentMessage (with failure response) update into DB successfully");
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        } finally {
            MDC.clear();
        }
    }

    @Override
    public String getCarrierDetails(String shipmentCorrelationId) {
        log.info("Entering  getCarrierDetails in SvcMessageServiceImpl " + shipmentCorrelationId);
        String carrierCode = "";
        ServiceMessage msgEntity = messageRepository.findByActiveAndShipmentCorrelationId(1, shipmentCorrelationId);
        if (!Objects.isNull(msgEntity)) {
            ShipmentMessage shipmentDetails = msgEntity.getMessage();

            if (!ObjectUtils.isEmpty(shipmentDetails.getMessageBody().getShipments()) &&
                    !ObjectUtils.isEmpty(shipmentDetails.getMessageBody().getShipments().get(0).getShipmentHeader()) &&
                    !ObjectUtils.isEmpty(shipmentDetails.getMessageBody().getShipments().get(0).getShipmentHeader().getCarrier()))
                carrierCode = shipmentDetails.getMessageBody().getShipments().get(0).getShipmentHeader().getCarrier()
                        .getCarrierCode();
        }

        log.info("Request cancel processing routed to {}  ", carrierCode);
        return carrierCode;
    }

    @Override
    public String retrieveOriginEntity(String shipmentId) {
        log.info("Entering  retrieveOriginEntity in SvcMessageServiceImpl # {}", shipmentId);
        ServiceMessage msgEntity = messageRepository.findByActiveAndShipmentCorrelationId(1, shipmentId);
        if (!Objects.isNull(msgEntity)) {
            log.info("Request cancel processing routed to # {}  ", msgEntity.getOriginEntity());
            return msgEntity.getOriginEntity();
        }
        return null;
    }

    @Override
    public String getCanceledShipment(String shipmentCorrelationId) {
        log.info("Entering  getCanceledShipment in SvcMessageServiceImpl " + shipmentCorrelationId);
        try {
            ServiceMessage msgEntity = messageRepository.findByActiveAndShipmentCorrelationId(1, shipmentCorrelationId);
            if (!Objects.isNull(msgEntity)) {
                return msgEntity.getShipmentStatus();
            } else {
                return SHIPMENT_NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    @Override
    public ShipmentMessage getShipmentMessageByCorrelationId(String shipmentCorrelationId) {
        log.info("Entering getShipmentMessage in SvcMessageServiceImpl {}", shipmentCorrelationId);
        try {
            ServiceMessage msgEntity = messageRepository.findByActiveAndShipmentCorrelationId(1, shipmentCorrelationId);
            if (!Objects.isNull(msgEntity)) {
                return msgEntity.getMessage();
            }
        } catch (Exception e) {
            log.error("Error Occurred {} ", ExceptionUtils.getStackTrace(e));
        }
        return null;
    }

    private static String getOriginEntityFromShipmentMessage(ShipmentMessage shipmentMessage) {
        if (shipmentMessage.getMessageHeader() != null
                && shipmentMessage.getMessageHeader().getMessageSources() != null
                && !shipmentMessage.getMessageHeader().getMessageSources().isEmpty()) {
            return shipmentMessage.getMessageHeader().getMessageSources().get(0).getEntityCode();
        }
        return null;
    }

    private static String getCarrierCodeFromShipmentMessage(ShipmentMessage shipmentMessage) {
        if (shipmentMessage.getMessageBody().getShipments() != null
                && !shipmentMessage.getMessageBody().getShipments().isEmpty()
                && shipmentMessage.getMessageBody().getShipments().get(0) != null
                && shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader() != null
                && shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getCarrier() != null) {
            return shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getCarrier().getCarrierCode();
        }
        return null;
    }
}
