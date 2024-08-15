package com.luluroute.ms.service.transformer;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;

public interface ShipmentTransformerService {

    void transform(ShipmentMessage message);
}
