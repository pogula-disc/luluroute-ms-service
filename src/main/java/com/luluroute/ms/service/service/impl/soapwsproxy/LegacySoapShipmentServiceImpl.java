package com.luluroute.ms.service.service.impl.soapwsproxy;

import com.enroutecorp.ws.inbound.ShipmentCancel;
import com.enroutecorp.ws.inbound.XmlShipmentCreateAndExecute;
import com.enroutecorp.ws.outbound.ShipmentCancelResponse;
import com.enroutecorp.ws.outbound.XmlShipmentCreateAndExecuteResponse;
import com.luluroute.ms.service.config.SoapUserProfile;
import com.luluroute.ms.service.config.XmlJsonConfig;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import com.luluroute.ms.service.helper.SoapContextHelper;
import com.luluroute.ms.service.soapwsclient.LegacySoapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;


@Service
@Slf4j
@RequiredArgsConstructor
public class LegacySoapShipmentServiceImpl {

    final LegacySoapClient legacySoapClient;
    private final SoapContextHelper soapContextHelper;
    private final LegacySoapAuthServiceImpl legacySoapAuthService;
    private final XmlJsonConfig xmlJsonConfig;
    private final LegacySoapHelper legacySoapHelper;


    public Node processXmlShipmentCreateAndExecute(XmlShipmentCreateAndExecute inboundRequest) throws LegacySoapWebServiceException {
        // Add token retrieved from legacy auth API's
        if (!xmlJsonConfig.getEnableTokenValidation() && StringUtils.isEmpty(inboundRequest.getToken()))
            inboundRequest.setToken(addAuthTokenToLegacyRequest());

        XmlShipmentCreateAndExecuteResponse legacyShipmentResponse =
                legacySoapClient.callXmlShipmentCreateAndExecute(inboundRequest);

        legacySoapHelper.parseXMLAndPersistShipmentToDB(legacyShipmentResponse,soapContextHelper.getSoapUserProfile().getDcEntityCode());

        return (Node) legacyShipmentResponse.getXmlShipmentCreateAndExecuteResult().getContent().get(0);
    }

    public Node processShipmentCancel(ShipmentCancel inboundRequest) {
        // Add token retrieved from legacy auth API's
        if (!xmlJsonConfig.getEnableTokenValidation() && StringUtils.isEmpty(inboundRequest.getToken()))
            inboundRequest.setToken(addAuthTokenToLegacyRequest());

        ShipmentCancelResponse legacyShipmentResponse =
                legacySoapClient.callShipmentCancel(inboundRequest);
        return (Node) legacyShipmentResponse.getShipmentCancelResult().getContent().get(0);
    }

    // Add token retrieved from legacy auth API's
    private String addAuthTokenToLegacyRequest() {
        String msg = "LegacySoapShipmentServiceImpl.addAuthTokenToLegacyRequest()";
        try {
            SoapUserProfile soapUserProfile = soapContextHelper.getSoapUserProfile();
            log.info("{}, User Profile # {}", msg, soapUserProfile.getUserEmail());
            LegacySoapAuthServiceImpl.AuthDetails authDetails =
                    legacySoapAuthService.getTokenForUser(soapUserProfile.getUserEmail(), soapUserProfile.getUserPassword());

            return authDetails.token();
        } catch (Exception exp) {
            log.error("Exception Occurred{} {} ", msg, exp);
            throw new ShipmentServiceException(String.format("Error while authentication for: %s", exp));
        }
    }

}
