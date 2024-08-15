package com.luluroute.ms.service.soapwsclient;

import com.enroutecorp.ws.outbound.*;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;
import org.springframework.ws.soap.client.core.SoapActionCallback;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

/**
 * Calls legacy SOAP services.
 * .
 * Request/response logging can be enabled via:
 *     logging.level.org.springframework.ws.client.MessageTracing.sent: DEBUG
 *     logging.level.org.springframework.ws.client.MessageTracing.received: TRACE
 * ****** Use with caution for performance reasons.
 */
public class LegacySoapClient extends WebServiceGatewaySupport {

    SoapActionCallback xmlShipmentCreateAndExecuteCallback =
            new SoapActionCallback(SOAP_LEGACY_ACTION_SHIPMENT_CREATE_AND_EXECUTE);
    SoapActionCallback xmlShipmentCancelCallback =
            new SoapActionCallback(SOAP_LEGACY_ACTION_SHIPMENT_CANCEL);
    SoapActionCallback authenticationValidateTokenCallback =
            new SoapActionCallback(SOAP_LEGACY_ACTION_AUTHENTICATION_VALIDATE_TOKEN);
    SoapActionCallback authenticationGetTokenCallback =
            new SoapActionCallback(SOAP_LEGACY_ACTION_AUTHENTICATION_GET_TOKEN);

    public AuthenticationValidateTokenResponse callAuthenticationValidateToken(String token) {
        AuthenticationValidateToken legacyRequest = new AuthenticationValidateToken();
        legacyRequest.setToken(token);
        return (AuthenticationValidateTokenResponse) getWebServiceTemplate()
                .marshalSendAndReceive(legacyRequest, authenticationValidateTokenCallback);
    }

    public AuthenticationGetTokenResponse callNewAuthenticationGetToken(String user, String password) {
        AuthenticationGetToken authenticationGetToken = new AuthenticationGetToken();
        authenticationGetToken.setUser(user);
        authenticationGetToken.setPassword(password);
        return (AuthenticationGetTokenResponse) getWebServiceTemplate()
                .marshalSendAndReceive(authenticationGetToken, authenticationGetTokenCallback);
    }

    public XmlShipmentCreateAndExecuteResponse callXmlShipmentCreateAndExecute(
            com.enroutecorp.ws.inbound.XmlShipmentCreateAndExecute inboundRequest) {
        XmlShipmentCreateAndExecute legacyRequest = new XmlShipmentCreateAndExecute();
        legacyRequest.setToken(inboundRequest.getToken());
        legacyRequest.setXml(inboundRequest.getXml());

        return (XmlShipmentCreateAndExecuteResponse) getWebServiceTemplate()
                .marshalSendAndReceive(legacyRequest, xmlShipmentCreateAndExecuteCallback);
    }

    public ShipmentCancelResponse callShipmentCancel(
            com.enroutecorp.ws.inbound.ShipmentCancel inboundRequest) {
        ShipmentCancel legacyRequest = new ShipmentCancel();
        legacyRequest.setToken(inboundRequest.getToken());
        legacyRequest.setShipmentId(inboundRequest.getShipmentId());

        return (ShipmentCancelResponse) getWebServiceTemplate()
                .marshalSendAndReceive(legacyRequest, xmlShipmentCancelCallback);
    }

}
