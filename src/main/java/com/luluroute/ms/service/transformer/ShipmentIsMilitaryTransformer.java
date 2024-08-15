package com.luluroute.ms.service.transformer;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;

import java.util.List;

public class ShipmentIsMilitaryTransformer {

    public static void transform(ShipmentMessage input, List<String> militaryStateCodes) {
        List<ShipmentInfo> shipments = input.getMessageBody().getShipments();
        for (ShipmentInfo eaShipment : shipments) {
            if (eaShipment.getShipmentHeader() != null &&
                    eaShipment.getShipmentHeader().getDestination() != null &&
                    eaShipment.getShipmentHeader().getDestination().getAddressTo() != null &&
                    eaShipment.getOrderDetails() != null) {
                LocationItem addressTo = eaShipment.getShipmentHeader().getDestination().getAddressTo();
                eaShipment.getOrderDetails().setMilitary(
                        String.valueOf(addressTo.getCountry()).equalsIgnoreCase("US") &&
                                militaryStateCodes.contains(String.valueOf(addressTo.getState())));
            }
        }
    }
}
