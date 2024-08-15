package com.luluroute.ms.service.soapwsendpoint;

import com.enroutecorp.ws.inbound.*;
import com.enroutecorp.ws.outbound.AuthenticationGetTokenResponse;
import com.enroutecorp.ws.outbound.AuthenticationValidateToken;
import com.enroutecorp.ws.outbound.AuthenticationValidateTokenResponse;
import com.luluroute.ms.service.service.ShipmentRedirectService;
import com.luluroute.ms.service.util.ShipmentCorrelationIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringReader;
import java.io.StringWriter;

import static com.luluroute.ms.service.soapwsendpoint.SoapWebServicesHandler.endContext;
import static com.luluroute.ms.service.soapwsendpoint.SoapWebServicesHandler.startContext;
import static com.luluroute.ms.service.util.ShipmentConstants.*;

/**
 * This is a thin SOAP/Web Services (WS) endpoint that proxy-routes to either Legacy 1.0 SOAP services or 2.0 REST
 * services.
 */
@Slf4j
@Endpoint
@RequiredArgsConstructor
public class SoapWebServicesProxyEndpoint {

    private final SoapWebServicesHandler soapWebServicesHandler;
    private final ShipmentRedirectService shipmentRedirectService;

    @PayloadRoot(namespace = SOAP_INBOUND_NAME_SPACE, localPart = SOAP_INBOUND_ACTION_SHIPMENT_CREATE_AND_EXECUTE)
    @ResponsePayload
    public XmlShipmentCreateAndExecuteResponse routeXmlShipmentCreateAndExecute(
            @RequestPayload XmlShipmentCreateAndExecute inboundRequest) {
        long startTime = startContext(inboundRequest, inboundRequest.getMessageCorrelationId());

        XmlShipmentCreateAndExecuteResponse response = new XmlShipmentCreateAndExecuteResponse();
        Object result = soapWebServicesHandler.handleSoapRequest(inboundRequest.getToken(), () ->
            shipmentRedirectService.redirectShipmentMessage(inboundRequest));
        response.setXmlShipmentCreateAndExecuteResult(result);

        endContext(response, startTime);
        return response;
    }

    @PayloadRoot(namespace = SOAP_INBOUND_NAME_SPACE, localPart = SOAP_INBOUND_ACTION_SHIPMENT_CANCEL)
    @ResponsePayload
    public ShipmentCancelResponse routeShipmentCancel(
            @RequestPayload ShipmentCancel inboundRequest) {
        long startTime = startContext(inboundRequest, inboundRequest.getMessageCorrelationId());
        MDC.put(X_SHIPMENT_CORRELATION_ID, inboundRequest.getShipmentId());
        if (!StringUtils.isNumeric(inboundRequest.getShipmentId())) {
            MDC.put(X_SHIPMENT_CORRELATION_ID, ShipmentCorrelationIdUtil.uuid64ToUuid(inboundRequest.getShipmentId()));
            log.info("Transformed from UUID64 # {} to UUID # {} ", inboundRequest.getShipmentId(), MDC.get(X_SHIPMENT_CORRELATION_ID));
        }

        ShipmentCancelResponse response = new ShipmentCancelResponse();
        Object result = soapWebServicesHandler.handleSoapRequest(inboundRequest.getToken(), () ->
                shipmentRedirectService.redirectShipmentCancelMessage(inboundRequest));
        response.setShipmentCancelResult(result);

        endContext(response, startTime);
        return response;
    }

    @PayloadRoot(namespace = SOAP_INBOUND_NAME_SPACE, localPart = SOAP_INBOUND_ACTION_AUTHENTICATE_VALIDATE_TOKEN)
    @ResponsePayload
    public AuthenticationValidateTokenResponse routeAuthValidateToken(
            @RequestPayload AuthenticationValidateToken inboundRequest) {
        return (AuthenticationValidateTokenResponse) soapWebServicesHandler.handleSoapAuthenticationValidateTokenRequest(inboundRequest.getToken());
    }

    @PayloadRoot(namespace = SOAP_INBOUND_NAME_SPACE, localPart = SOAP_INBOUND_ACTION_AUTHENTICATE_GET_TOKEN)
    @ResponsePayload
    public AuthenticationGetTokenResponse routeAuthGetToken(
            @RequestPayload AuthenticationGetToken inboundRequest) {
        return  (AuthenticationGetTokenResponse) soapWebServicesHandler.handleSoapAuthenticationGetTokenRequest(inboundRequest.getUser(), inboundRequest.getPassword());
    }


}

