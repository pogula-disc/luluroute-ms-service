package com.luluroute.ms.service.service;

import com.enroutecorp.ws.inbound.ShipmentCancel;
import com.enroutecorp.ws.inbound.XmlShipmentCreateAndExecute;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import org.w3c.dom.Node;

import java.util.concurrent.CompletableFuture;

public interface ShipmentRedirectService {

    CompletableFuture<ShipmentServiceResponse> redirectShipmentMessage(ShipmentMessage shipmentMessage);
    Node redirectShipmentMessage(XmlShipmentCreateAndExecute xmlShipmentCreateAndExecute) throws LegacySoapWebServiceException;
    Node redirectShipmentCancelMessage(ShipmentCancel shipmentCancel) throws LegacySoapWebServiceException;

}
