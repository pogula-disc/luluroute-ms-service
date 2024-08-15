package com.luluroute.ms.service.service;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ServiceMessageDto;
import com.luluroute.ms.service.dto.ServiceMessageResponse;
import com.luluroute.ms.service.dto.ShipmentSearchDto;

import java.util.concurrent.CompletableFuture;

public interface SvcMessageService {
    ServiceMessageResponse createServiceMessage(ServiceMessageDto serviceMessage);

    ServiceMessageResponse cancelServiceMessage(String shipCorrId);

    ServiceMessageResponse searchServiceMessage(ShipmentSearchDto searchDto);

    CompletableFuture<Void>  updateShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution);

    CompletableFuture<Void> saveShipmentMessage(ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution);

    void saveLegacyShipmentMessage(String shipmentId , String originEntity, String carrierCode);

    CompletableFuture<Void> updateShipmentCreateValidationFailure(
            ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution);

    CompletableFuture<Void> updateShipmentCreateFailure(
            ShipmentMessage shipmentMessage, String shipCorrelationId, CompletableFuture<Void> svcDbExecution);

    String getCarrierDetails(String shipmentCorrelationId);

    String retrieveOriginEntity(String shipmentCorrelationId);

    String getCanceledShipment(String shipmentCorrelationId);

    ShipmentMessage getShipmentMessageByCorrelationId(String shipmentCorrelationId);
}
