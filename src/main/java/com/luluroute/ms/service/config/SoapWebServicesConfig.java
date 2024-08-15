package com.luluroute.ms.service.config;

import com.luluroute.ms.service.soapwsclient.LegacySoapClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadRootSmartSoapEndpointInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

import javax.xml.soap.SOAPMessage;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

@EnableWs
@Configuration
public class SoapWebServicesConfig extends WsConfigurerAdapter {

    @Value("${legacy.shipmentSoapUrl}")
    private String legacySoapEnrouteUrl;

    @Value("${legacy.soap.connection.readTimeout}")
    private int legacySoapReadTimeout;

    @Value("${legacy.soap.connection.connectionTimeout}")
    private int legacySoapConnectionTimeout;

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, SOAP_INBOUND_URI + "/*");
    }

    @Bean(name = SOAP_INBOUND_ACTION_SHIPMENT_CREATE_AND_EXECUTE)
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema xmlShipmentCreateAndExecuteSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setLocationUri(SOAP_INBOUND_URI);
        wsdl11Definition.setPortTypeName(SOAP_INBOUND_PORT_TYPE);
        wsdl11Definition.setTargetNamespace(SOAP_INBOUND_NAME_SPACE);
        wsdl11Definition.setSchema(xmlShipmentCreateAndExecuteSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema xmlShipmentCreateAndExecuteSchema() {
        return new SimpleXsdSchema(new ClassPathResource(SOAP_INBOUND_XSD));
    }

    @Bean
    public LegacySoapClient legacySoapClient(Jaxb2Marshaller marshaller) {
        LegacySoapClient legacySoapClient = new LegacySoapClient();
        legacySoapClient.setDefaultUri(legacySoapEnrouteUrl);
        legacySoapClient.setMarshaller(marshaller);
        legacySoapClient.setUnmarshaller(marshaller);
        for(WebServiceMessageSender sender : legacySoapClient.getWebServiceTemplate().getMessageSenders()) {
            HttpUrlConnectionMessageSender httpSender = (HttpUrlConnectionMessageSender) sender;
            httpSender.setReadTimeout(Duration.ofMillis(legacySoapReadTimeout));
            httpSender.setConnectionTimeout(Duration.ofMillis(legacySoapConnectionTimeout));
        }
        legacySoapClient.setInterceptors(new ClientInterceptor[]{new LoggingSoapInterceptor()});
        return legacySoapClient;
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath(SOAP_LEGACY_PACKAGE);
        return marshaller;
    }

    @Bean (name = "messageFactory")
    public SaajSoapMessageFactory messageFactory () {
        Map<String, Object> props = new HashMap<>();
        props.put(SOAPMessage.WRITE_XML_DECLARATION, "true");

        SaajSoapMessageFactory msgFactory = new SaajSoapMessageFactory();
        msgFactory.setMessageProperties(props);
        msgFactory.setSoapVersion(org.springframework.ws.soap.SoapVersion.SOAP_11);

        return msgFactory;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) {
        // register global interceptor
        interceptors.add(new CustomEndpointInterceptor());

        // register endpoint specific interceptor
        interceptors.add(new PayloadRootSmartSoapEndpointInterceptor(
                new CustomEndpointInterceptor(),
                CustomEndpointInterceptor.SOAP_ENV_NAMESPACE,
                CustomEndpointInterceptor.PREFERRED_PREFIX));
    }

}
