package com.luluroute.ms.service.transformer;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class ShipmentTransformerServiceImpl implements ShipmentTransformerService {

    @Autowired
    private AppConfig appConfig;
    public void transform(ShipmentMessage message) {
        if (appConfig.getMilitaryStateCodes() != null && !appConfig.getMilitaryStateCodes().isEmpty()) {
            ShipmentIsMilitaryTransformer.transform(message, appConfig.getMilitaryStateCodes());
        }
        ShipmentIsPOBoxTransformer.transform(message);
        ShipmentWeightTypeTransformer.transform(message);
    }
}
