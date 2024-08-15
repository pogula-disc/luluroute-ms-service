package com.luluroute.ms.service.transformer;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Slf4j
public class ShipmentWeightTypeTransformer {

    static String LBS = "lbs";
    static String LB = "LB";

    public static void transform(ShipmentMessage input) {
        List<ShipmentInfo> shipments = input.getMessageBody().getShipments();
        shipments.forEach(shipmentInfo -> {
                    if (shipmentInfo.getShipmentPieces() != null) {
                        shipmentInfo.getShipmentPieces().forEach(shipmentPieceInfo -> {
                                    // Replace lbs with LB
                                    if (StringUtils.isNotEmpty(shipmentPieceInfo.getWeightDetails().getUom()) &&
                                            shipmentPieceInfo.getWeightDetails().getUom().equalsIgnoreCase(LBS))
                                        shipmentPieceInfo.getWeightDetails().setUom(LB);

                                    // Replace lbs with LB
                                    shipmentPieceInfo.getCartonsDetails().forEach(cartonInfo -> {
                                        if (StringUtils.isNotEmpty(cartonInfo.getWeightDetails().getUom()) &&
                                                cartonInfo.getWeightDetails().getUom().equalsIgnoreCase(LBS))
                                            cartonInfo.getWeightDetails().setUom(LB);
                                    });

                                }
                        );
                    }
                }
        );
    }
}
