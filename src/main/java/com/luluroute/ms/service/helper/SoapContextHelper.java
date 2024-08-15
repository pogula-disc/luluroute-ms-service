package com.luluroute.ms.service.helper;

import com.enroutecorp.ws.inbound.*;
import com.enroutecorp.ws.outbound.AuthenticationValidateTokenResponse;
import com.luluroute.ms.service.config.SoapUserProfile;
import com.luluroute.ms.service.config.UserProfileConfig;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

import static com.luluroute.ms.service.util.ShipmentConstants.*;
import static java.lang.Byte.parseByte;
import static java.lang.String.format;

@Component
@Slf4j
@RequiredArgsConstructor
public class SoapContextHelper {

    public static final JAXBContext CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE_RESPONSE;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_CONTENT;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_ERROR;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_ERROR_CONTENT;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_SUCCESS;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_SUCCESS_CONTENT;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_CANCEL;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_CANCEL_RESPONSE;
    public static final JAXBContext CONTEXT_XML_SHIPMENT_CANCEL_CONTENT;
    public static final JAXBContext CONTEXT_AUTHENTICATION_GET_TOKEN;
    public static final JAXBContext CONTEXT_AUTHENTICATION_GET_TOKEN_RESPONSE;

    static {
        try {
            CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE = JAXBContext.newInstance(XmlShipmentCreateAndExecute.class);
            CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE_RESPONSE = JAXBContext.newInstance(XmlShipmentCreateAndExecuteResponse.class);
            CONTEXT_XML_SHIPMENT_CONTENT = JAXBContext.newInstance(Shipments.class);
            CONTEXT_XML_SHIPMENT_ERROR = JAXBContext.newInstance(ShipmentError.class);
            CONTEXT_XML_SHIPMENT_ERROR_CONTENT = JAXBContext.newInstance(com.enroutecorp.ws.inbound.content.ShipmentError.class);
            CONTEXT_XML_SHIPMENT_SUCCESS = JAXBContext.newInstance(com.enroutecorp.ws.inbound.ShipmentSuccess.class);
            CONTEXT_XML_SHIPMENT_SUCCESS_CONTENT = JAXBContext.newInstance(com.enroutecorp.ws.inbound.content.ShipmentSuccess.class);
            CONTEXT_XML_SHIPMENT_CANCEL = JAXBContext.newInstance(com.enroutecorp.ws.inbound.ShipmentCancelContent.class);
            CONTEXT_XML_SHIPMENT_CANCEL_RESPONSE = JAXBContext.newInstance(com.enroutecorp.ws.inbound.ShipmentCancelResponse.class);
            CONTEXT_XML_SHIPMENT_CANCEL_CONTENT = JAXBContext.newInstance(com.enroutecorp.ws.inbound.content.ShipmentCancelContent.class);
            CONTEXT_AUTHENTICATION_GET_TOKEN = JAXBContext.newInstance(AuthenticationGetToken.class);
            CONTEXT_AUTHENTICATION_GET_TOKEN_RESPONSE = JAXBContext.newInstance(AuthenticationValidateTokenResponse.class);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
    }

    private final UserProfileConfig userProfileConfig;

    /**
     * Wraps node in one additional nested layer (because generated XML object property will absorb outermost DOM level).
     */
    public static Node wrapNode(Node node) {
        Node newResponseNode = node.getOwnerDocument().createElement(node.getLocalName());
        for(int i = 0; i < node.getAttributes().getLength(); i ++) {
            Node newAttribute = node.getAttributes().item(i).cloneNode(true);
            newResponseNode.getAttributes().setNamedItem(newAttribute);
        }

        // Remove additional attributes
        if(null != node.getAttributes()) {
            if(node.getAttributes().getNamedItem("server")  != null)
                node.getAttributes().removeNamedItem("server");
            if(node.getAttributes().getNamedItem("current_time")  != null)
                node.getAttributes().removeNamedItem("current_time");
            if(node.getAttributes().getNamedItem("elapsed_time")  != null)
                node.getAttributes().removeNamedItem("elapsed_time");
        }

        while (node.hasChildNodes()) {
            newResponseNode.appendChild(node.getFirstChild());
        }
        node.getOwnerDocument().getDocumentElement().appendChild(newResponseNode);
        return node;
    }

    public static void logInboundSoapRequest(Object inboundRequest) {
        logXml(CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE, inboundRequest, ENTERING_MESSAGE_SOAP);
    }

    public static void logInboundSoapResponse(Object response) {
        if(response instanceof XmlShipmentCreateAndExecuteResponse) {
            logXml(CONTEXT_XML_SHIPMENT_CREATE_AND_EXECUTE_RESPONSE, response, EXITING_MESSAGE_SOAP);
        } else {
            logXml(CONTEXT_XML_SHIPMENT_CANCEL_RESPONSE, response, EXITING_MESSAGE_SOAP);
        }
    }

    public static void logXml(JAXBContext context, Object xml, String message) {
        try {
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter out = new StringWriter();
            marshaller.marshal(xml, out);
            log.info(message, out);
        } catch (JAXBException e) {
            log.error(message,
                    format("Unexpected error while attempting to log XML: %s", e.getMessage()));
        }
    }

    public static Shipments parseInboundShipmentContent(String xml) throws LegacySoapWebServiceException {
        try {
            log.debug("Parsing XML shipment content");
            Unmarshaller unmarshaller = CONTEXT_XML_SHIPMENT_CONTENT.createUnmarshaller();
            return (Shipments) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            if(e.getCause() instanceof SAXParseException) {
                throw new LegacySoapWebServiceException(format("Unexpected XML string parse exception: %s",
                        e.getCause().getMessage()), e, null, false);
            }
            throw new LegacySoapWebServiceException("Unexpected XML string parse exception", e, null, true);
        }
    }

    public static Node buildErrorResponse(String error) {
        return wrapNode(buildErrorResponseContent(error));
    }

    public static Node buildErrorResponse(Exception e) {
        return wrapNode(buildErrorResponseContent(ExceptionUtils.getStackTrace(e)));
    }

    public static Node buildErrorResponseContent(String errorMessage) {
        try {
            var shipmentError = buildShipmentErrorContent(errorMessage);
            return marshalShipmentErrorToNode(shipmentError);
        } catch (Exception e2) {
            log.error("Error while building error response", e2);
            throw new ShipmentServiceException(format("Error while building error response for original error %s",
                    errorMessage), e2);
        }
    }

    private static com.enroutecorp.ws.inbound.content.ShipmentError buildShipmentErrorContent(String message) {
        log.debug("Building error shipment XML response content");
        var error = new com.enroutecorp.ws.inbound.content.ShipmentError.Shipment.Error();
        error.setMessage(message);
        var shipment = new com.enroutecorp.ws.inbound.content.ShipmentError.Shipment();
        shipment.setError(error);
        shipment.setStatus(parseByte("0"));
        var shipmentError = new com.enroutecorp.ws.inbound.content.ShipmentError();
        shipmentError.setShipment(shipment);
        return shipmentError;
    }

    private static Node marshalShipmentErrorToNode(com.enroutecorp.ws.inbound.content.ShipmentError shipmentError)
            throws JAXBException {
        Marshaller marshaller = CONTEXT_XML_SHIPMENT_ERROR_CONTENT.createMarshaller();
        StringWriter out = new StringWriter();
        marshaller.marshal(shipmentError, out);
        Unmarshaller unmarshaller = CONTEXT_XML_SHIPMENT_ERROR.createUnmarshaller();
        ShipmentError untypedShipmentError = (ShipmentError) unmarshaller.unmarshal(new StringReader(out.toString()));
        return (Node) untypedShipmentError.getShipment();
    }

    public static Node buildSuccessResponse(com.enroutecorp.ws.inbound.content.ShipmentSuccess shipmentSuccess) {
        try {
            log.debug("Building successful shipment XML response content");
            Marshaller marshaller = CONTEXT_XML_SHIPMENT_SUCCESS_CONTENT.createMarshaller();
            StringWriter out = new StringWriter();
            marshaller.marshal(shipmentSuccess, out);
            Unmarshaller unmarshaller = CONTEXT_XML_SHIPMENT_SUCCESS.createUnmarshaller();
            ShipmentSuccess untypedShipmentSuccess = (ShipmentSuccess) unmarshaller.unmarshal(new StringReader(out.toString()));
            return (Node) untypedShipmentSuccess.getShipment();
        } catch (Exception e2) {
            log.error("Error while building success response", e2);
            throw new ShipmentServiceException(format("Error while building success response for: %s",
                    shipmentSuccess.toString()), e2);
        }
    }

    public static Node buildCancelResponse(com.enroutecorp.ws.inbound.content.ShipmentCancelContent shipmentCancel) {
        try {
            log.debug("Building cancel shipment XML response content");
            Marshaller marshaller = CONTEXT_XML_SHIPMENT_CANCEL_CONTENT.createMarshaller();
            StringWriter out = new StringWriter();
            marshaller.marshal(shipmentCancel, out);
            Unmarshaller unmarshaller = CONTEXT_XML_SHIPMENT_CANCEL.createUnmarshaller();
            ShipmentCancelContent untypedShipmentCancel = (ShipmentCancelContent) unmarshaller.unmarshal(new StringReader(out.toString()));
            return (Node) untypedShipmentCancel.getShipment();
        } catch (Exception e2) {
            log.error("Error while building cancel response", e2);
            throw new ShipmentServiceException(format("Error while building cancel response for: %s",
                    shipmentCancel.toString()), e2);
        }
    }

    public String getEntityCodeFromUser() throws LegacySoapWebServiceException {
        SoapUserProfile dcProfile = userProfileConfig.getSoapUsersById().get(MDC.get(USER_ID));
        if(dcProfile == null)
            dcProfile = userProfileConfig.getSoapUsersByZip().get(MDC.get(USER_ZIP_CODE));

        if(dcProfile == null)
            dcProfile = userProfileConfig.getSoapUsersByDCEntity().get(MDC.get(USER_DC_ENTITY));

        if(dcProfile == null) {
            throw new LegacySoapWebServiceException(String.format("No DC entity configured for %s or zip (%s)", MDC.get(USER_ID), MDC.get(USER_ZIP_CODE)),
                    null, null, false);
        }
        return dcProfile.getDcEntityCode();
    }

    public String getSoapUserFromZipCode(String zipCode) throws LegacySoapWebServiceException {
        SoapUserProfile dcProfile = userProfileConfig.getSoapUsersByZip().get(zipCode);
        if(dcProfile == null) {
            throw new LegacySoapWebServiceException(String.format("No DC entity configured for %s", zipCode),
                    null, null, false);
        }
        return dcProfile.getUserEmail();
    }

    public SoapUserProfile getSoapUserProfile() throws LegacySoapWebServiceException {
        SoapUserProfile dcProfile = userProfileConfig.getSoapUsersById().get(MDC.get(USER_ID));
        if(dcProfile == null)
            dcProfile = userProfileConfig.getSoapUsersByZip().get(MDC.get(USER_ZIP_CODE));

        if(dcProfile == null)
            dcProfile = userProfileConfig.getSoapUsersByDCEntity().get(MDC.get(USER_DC_ENTITY));


        return dcProfile;
    }

}

