package com.luluroute.ms.service.kafka;

import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.MessageHeaderInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.RequestInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.logistics.luluroute.domain.Shipment.Shared.StatusItem;

import java.time.Instant;
import java.util.List;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

public class ShipmentCancelResponseBuilder {

    public static ShipmentMessage buildCancelShipmentResponse(String messageCorrelationId , String shipmentCorrelationId, String responseMessage) {
        long date = Instant.now().getEpochSecond();

        RequestInfo.RequestInfoBuilder requestInfoBuilder = RequestInfo.builder();
        requestInfoBuilder.requestType(String.valueOf(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE))
                .requestDate(date)
                .response(ResponseItem.builder()
                        .responseCode(CANCEL_SHIPMENT_SUCCESS_RESPONSE_CODE)
                        .responseMessage(responseMessage)
                        .responseDate(date)
                        .build());

        StatusItem.StatusItemBuilder messageStatusBuilder = StatusItem.builder();
        messageStatusBuilder.status(CANCEL_SHIPMENT_SUCCESS_REQUEST_TYPE)
                .statusDate(date);

        MessageHeaderInfo.MessageHeaderInfoBuilder messageHeaderBuilder = MessageHeaderInfo.builder();
        messageHeaderBuilder
                .messageCorrelationId(messageCorrelationId)
                .sequence(1)
                .totalSequence(1)
                .messageDate(date);
        MessageBodyInfo.MessageBodyInfoBuilder messageBodyInfoBuilder = MessageBodyInfo.builder();

        ShipmentInfo shipmentsResponse = ShipmentInfo.builder()
                .shipmentHeader(ShipmentHeader.builder()
                        .shipmentCorrelationId(shipmentCorrelationId)
                        .build())
                .shipmentStatus(messageStatusBuilder.build())
                .build();

        messageBodyInfoBuilder.shipments(List.of(shipmentsResponse));

        return ShipmentMessage.builder().
                RequestHeader(requestInfoBuilder.build())
                .MessageStatus(messageStatusBuilder.build())
                .MessageHeader(messageHeaderBuilder.build())
                .MessageBody(messageBodyInfoBuilder.build())
                .build();
    }
}
