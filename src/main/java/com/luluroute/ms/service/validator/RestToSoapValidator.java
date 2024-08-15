package com.luluroute.ms.service.validator;

import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RestToSoapValidator {
    public static void checkErrors(ShipmentServiceResponse jsonResponse) throws LegacySoapWebServiceException {
        ResponseItem responseItem = jsonResponse.getShipmentMessage().getRequestHeader().getResponse();
        if (!FAILURE_RESPONSE.equals(responseItem.getResponseCode())) {
            return;
        }

        StringBuilder error = new StringBuilder();
        if (StringUtils.isNotEmpty(jsonResponse.getMessage()) &&
                !StringUtils.equals(jsonResponse.getMessage(), RESPONSE_PERSISTED_SUCCESSFULLY)) {
            error.append(jsonResponse.getMessage());
            error.append("\n");
        }
        if (StringUtils.isNotEmpty(responseItem.getResponseMessage())) {
            error.append(responseItem.getResponseMessage());
            error.append("\n");
        }
        if (responseItem.getExtended() != null && !responseItem.getExtended().isEmpty()) {
            error.append(responseItem.getExtended().stream()
                    .map(ItemInfo::getValue)
                    .collect(Collectors.joining("\n")));
        }
        if (error.isEmpty()) {
            error.append(SOAP_UNKNOWN_ERROR); // to match Legacy
        }
        error.insert(0, SOAP_NO_RECOMMENDED_SERVICE_ERROR + "\n"); // to match Legacy
        throw new LegacySoapWebServiceException(error.toString(), null, null, false);

    }
}
