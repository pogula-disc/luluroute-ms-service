package com.luluroute.ms.service.service;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.response.entity.EntityProfile;
import com.luluroute.ms.service.dto.*;
import org.json.JSONException;
import org.springframework.http.HttpHeaders;

import java.io.UnsupportedEncodingException;

public interface SvcDetailService {

    ShipmentServiceResponse saveShipmentServiceDetails(ShipmentMessage shipmentMessage);

    ShipmentServiceResponse updateShipmentStatus(ShipmentStatusDto shipmentStatus);

    ShipmentServiceResponse updateShipmentCancellation(ShipmentCancellationDto cancellationDto);

    ShipmentServiceResponse updateBillingDetails(ShipmentBillingDto billingDto);

    ShipmentQueryResponse searchShipmentDetails(ShipmentSearchDto searchDto);
    
	boolean getClientIdAfterTokenValidation(HttpHeaders headers, ShipmentMessage shipmentMessage)throws JSONException,UnsupportedEncodingException;

	EntityProfile getCallEntityDcService(HttpHeaders headers, ShipmentMessage shipmentMessage);
}
