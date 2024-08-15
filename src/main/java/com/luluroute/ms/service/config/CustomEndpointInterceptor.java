package com.luluroute.ms.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.interceptor.EndpointInterceptorAdapter;
import org.springframework.ws.soap.saaj.SaajSoapMessage;

import javax.xml.soap.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
@Slf4j
public class CustomEndpointInterceptor extends EndpointInterceptorAdapter { 
    public static final String SOAP_ENV_NAMESPACE = "http://schemas.xmlsoap.org/soap/envelope/";
    public static final String PREFERRED_PREFIX = "soap";

    public static final String XMLSCHEMA_INS_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String XMLSCHEMA_INS_PREFIX = "xsi";

    public static final String XMLSCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    public static final String XMLSCHEMA_PREFIX = "xsd";

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception {
        log.debug("Endpoint Response Handling");
        SaajSoapMessage soapResponse = (SaajSoapMessage) messageContext.getResponse();
        alterSoapEnvelope(soapResponse);
        return super.handleResponse(messageContext, endpoint);
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception {
        log.debug("Endpoint Response fault Handling");
        SaajSoapMessage soapResponse = (SaajSoapMessage) messageContext.getResponse();
        alterSoapEnvelope(soapResponse);
        return super.handleFault(messageContext, endpoint);
    }

    private void alterSoapEnvelope(SaajSoapMessage soapResponse) {
        try {
            SOAPMessage soapMessage = soapResponse.getSaajMessage();
            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope envelope = soapPart.getEnvelope();
            SOAPHeader header = soapMessage.getSOAPHeader();
            SOAPBody body = soapMessage.getSOAPBody();
            SOAPFault fault = body.getFault();
            envelope.removeNamespaceDeclaration(envelope.getPrefix());
            envelope.addNamespaceDeclaration(PREFERRED_PREFIX, SOAP_ENV_NAMESPACE);
            envelope.addNamespaceDeclaration(XMLSCHEMA_INS_PREFIX, XMLSCHEMA_INS_NAMESPACE );
            envelope.addNamespaceDeclaration(XMLSCHEMA_PREFIX, XMLSCHEMA_NAMESPACE );
            envelope.setPrefix(PREFERRED_PREFIX);
            header.setPrefix(PREFERRED_PREFIX);
            body.setPrefix(PREFERRED_PREFIX);
            addDesiredBodyNamespaceEntries(body.getChildElements());

            removeUndesiredBodyNamespaceEntries(body.getChildElements());
            if (fault != null) {
                fault.setPrefix(PREFERRED_PREFIX);
            }
        } catch (SOAPException e) {
            e.printStackTrace();
        }
    }

    private void addDesiredBodyNamespaceEntries(Iterator childElements) {
        while (childElements.hasNext()) {
            final Object childElementNode = childElements.next();
            if (childElementNode instanceof SOAPElement) {
                SOAPElement soapElement = (SOAPElement) childElementNode;

                // set desired namespace body element prefix
                soapElement.setPrefix("");

                // recursively set desired namespace prefix entries in child elements
                addDesiredBodyNamespaceEntries(soapElement.getChildElements());
            }
        }
    }

    private void removeUndesiredBodyNamespaceEntries(Iterator childElements) {
        while (childElements.hasNext()) {
            final Object childElementNode = childElements.next();
            if (childElementNode instanceof SOAPElement) {
                SOAPElement soapElement = (SOAPElement) childElementNode;

                // we remove any prefix/namespace entries added by JAX-WS in the body element that is not the one we want
                for (String prefix : getNamespacePrefixList(soapElement.getNamespacePrefixes())) {
                    if (prefix != null && ! "tns".equals(prefix)) {
                        soapElement.removeNamespaceDeclaration(prefix);
                    }
                }

                // recursively remove prefix/namespace entries in child elements
                removeUndesiredBodyNamespaceEntries(soapElement.getChildElements());
            }
        }
    }

    private Set<String> getNamespacePrefixList(Iterator namespacePrefixIter) {
        Set<String> namespacePrefixesSet = new HashSet<>();
        while (namespacePrefixIter.hasNext()) {
            namespacePrefixesSet.add((String) namespacePrefixIter.next());
        }
        return namespacePrefixesSet;
    }
}
