package com.luluroute.ms.service.transformer;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ShipmentIsPOBoxTransformer {

    static String PO_BOX = "pobox";

    public static void transform(ShipmentMessage input) {
        List<ShipmentInfo> shipments = input.getMessageBody().getShipments();
        for (ShipmentInfo eaShipment : shipments) {
            if (eaShipment.getShipmentHeader() != null &&
                    eaShipment.getShipmentHeader().getDestination() != null &&
                    eaShipment.getShipmentHeader().getDestination().getAddressTo() != null &&
                    eaShipment.getOrderDetails() != null) {
                LocationItem addressTo = eaShipment.getShipmentHeader().getDestination().getAddressTo();
                String addressToVal = addressTo.getDescription1() != null ? addressTo.getDescription1().toLowerCase() : "";
                String addressToVal2 = addressTo.getDescription2() != null ? addressTo.getDescription2().toLowerCase() : "";

                eaShipment.getOrderDetails().setPOBox(addressToVal.replaceAll("[^a-zA-Z0-9]", "").toLowerCase().contains(PO_BOX) ||
                        addressToVal2.replaceAll("[^a-zA-Z0-9]", "").toLowerCase().contains(PO_BOX));

                log.info("Shipment address POBox? : {} ", eaShipment.getOrderDetails().isPOBox);
            }
        }
    }
}
