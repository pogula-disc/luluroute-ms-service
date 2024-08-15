package com.luluroute.ms.service.mapper;

import com.enroutecorp.ws.inbound.Shipments;
import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.BillingInfo;
import com.logistics.luluroute.domain.Shipment.Service.OriginInfo;
import com.logistics.luluroute.domain.Shipment.Service.RequestInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.logistics.luluroute.domain.Shipment.Shared.StatusItem;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

@RequiredArgsConstructor
@Component
@Slf4j
/* Mapping for XML > JSON. Mapping design as of 4/22/24 can be found at XML_TO_JSON_MAPPING.xlsx in 2.0 Sharepoint. */
public class XmlJsonShipmentRequestMapper {

    private final XmlJsonShipmentComponentMapper xmlComponentMapper;

    public ShipmentMessage mapShipmentCreateToJson(
            Shipments.Shipment xmlShipment, String xmlTimeZone, EntityPayload originEntity, Instant today) {
        log.debug("Beginning inbound XML Shipment mapping to JSON Shipment");
        ShipmentMessage jsonShipmentMessage = xmlComponentMapper
                .buildFieldsFromXml(xmlShipment, xmlTimeZone, originEntity);
        return addRemainingDefaultCreateFields(addMetaInfo(true, jsonShipmentMessage, originEntity, today), today);
    }

    public ShipmentMessage mapShipmentCancelToJson(EntityPayload originEntity, Instant today) {
        log.debug("Beginning inbound XML Cancel Shipment mapping to JSON Cancel Shipment");
        ShipmentMessage jsonShipmentMessage = buildCancelShipmentMessage(originEntity, today);
        return addMetaInfo(false, jsonShipmentMessage, originEntity, today);
    }

    /** Add any fields that cannot be sourced from the XML **/

    private static ShipmentMessage addMetaInfo(
            boolean isCreate, ShipmentMessage shipmentMessage, EntityPayload entityPayload, Instant today) {
        shipmentMessage.setMessageStatus(buildDefaultStatus(isCreate, today));
        shipmentMessage.setMessageHeader(buildDefaultMessageHeader(entityPayload, today));
        shipmentMessage.setRequestHeader(buildDefaultRequestHeader(isCreate, today));
        return shipmentMessage;
    }

    private static ShipmentMessage addRemainingDefaultCreateFields(ShipmentMessage shipmentMessage, Instant today) {
        log.debug("Mapping remaining JSON fields from default values");
        ShipmentInfo shipmentInfo = shipmentMessage.getMessageBody().getShipments().get(0);
        // Not the actual order created date, defaulting per architect
        shipmentInfo.getOrderDetails().setOrderCreatedDate(today.getEpochSecond());
        shipmentInfo.getOrderDetails().setIntegration(INTEGRATION);
        shipmentInfo.getOrderDetails().setBillingDetails(BillingInfo.builder().build());
        shipmentInfo.setShipmentStatus(buildDefaultStatus(true, today));
        return shipmentMessage;
    }

    private static StatusItem buildDefaultStatus(boolean isCreate, Instant today) {
        return StatusItem.builder()
                .status(isCreate ? SHIPMENT_STATUS_NEW : CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE)
                .statusDate(today.getEpochSecond())
                .build();
    }

    private static RequestInfo buildDefaultRequestHeader(boolean isCreate, Instant today){
        return RequestInfo.builder()
                .requestDate(today.getEpochSecond())
                .expireDate(0)
                .requestType(isCreate ? REQUEST_TYPE_SHIPMENT : String.valueOf(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE))
                .build();
    }

    private static MessageHeaderInfo buildDefaultMessageHeader(EntityPayload entityPayload, Instant today) {
        return MessageHeaderInfo.builder()
                .messageDate(today.getEpochSecond())
                .sequence(1)
                .totalSequence(1)
                .messageCorrelationId(MDC.get(X_CORRELATION_ID))
                .messageSources(List.of(EntityRole.builder()
                                .roleType(ROLE_TYPE_DC_PRIMARY)
                                .entityCode(entityPayload.getEntityCode())
                        .build()))
                .build();
    }

    private static ShipmentMessage buildCancelShipmentMessage(EntityPayload originEntity, Instant today) {
        OriginInfo originInfo = new OriginInfo();
        originInfo.setEntityCode(originEntity.getEntityCode());

        ShipmentHeader shipmentHeader = new ShipmentHeader();
        shipmentHeader.setOrigin(originInfo);
        shipmentHeader.setShipmentCorrelationId(MDC.get(X_SHIPMENT_CORRELATION_ID));

        StatusItem shipmentStatus = buildDefaultStatus(false, today);
        shipmentStatus.setStatus(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE);

        ShipmentInfo shipmentInfo = new ShipmentInfo();
        shipmentInfo.setShipmentHeader(shipmentHeader);
        shipmentInfo.setShipmentStatus(shipmentStatus);

        MessageBodyInfo messageBodyInfo = new MessageBodyInfo();
        messageBodyInfo.setShipments(List.of(shipmentInfo));

        ShipmentMessage shipmentMessage = new ShipmentMessage();
        shipmentMessage.setMessageBody(messageBodyInfo);
        return shipmentMessage;
    }

}
