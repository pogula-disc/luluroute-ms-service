package com.luluroute.ms.service.exception;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;

public class ShipmentCancellationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private ShipmentMessage shipmentMessage;

    public ShipmentCancellationException(String message) {
        super(message);
    }

    public ShipmentCancellationException(String message, ShipmentMessage shipmentMessage) {
        super(message);
        this.shipmentMessage = shipmentMessage;
    }

    public ShipmentMessage getShipmentMessage() {
        return shipmentMessage;
    }
}
