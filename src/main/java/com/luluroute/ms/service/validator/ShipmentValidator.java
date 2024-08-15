package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;

public interface ShipmentValidator {
    boolean isPayloadValid(ShipmentMessage shipmentMessage);
}
