package com.luluroute.ms.service.service;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;

public interface LegacyShipmentService {

    ShipmentServiceResponse processShipmentMessage(ShipmentMessage shipmentMessage);

}
