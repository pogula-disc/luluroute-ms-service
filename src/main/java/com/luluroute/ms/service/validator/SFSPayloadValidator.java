package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.config.AppConfig;
import org.springframework.stereotype.Service;

@Service
@Validators(command = ShipmentValidatorType.SFS_SHIPMENT_VALIDATOR)
public class SFSPayloadValidator extends ShipmentValidatorImpl {

    public SFSPayloadValidator(AppConfig appConfig) {
        super(appConfig);
    }

    public boolean isPayloadValid(ShipmentMessage shipmentMessage) {
        // Added for interface implementation
        return super.isPayloadValid(shipmentMessage);
    }

}
