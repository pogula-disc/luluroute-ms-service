package com.luluroute.ms.service.service.impl.soapwsproxy;

import com.enroutecorp.ws.outbound.XmlShipmentCreateAndExecuteResponse;
import com.luluroute.ms.service.service.SvcMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class LegacySoapHelper {
    @Autowired
    private SvcMessageService svcMessageService;
    @Autowired
    Executor svcDBExecutor;

    String SHIPMENT_ID_TAG = "/xml_shipment_create_and_executeResponse/xml_shipment_create_and_executeResult/shipment/information/id/text()";
    String CARRIER_NAME_TAG = "/xml_shipment_create_and_executeResponse/xml_shipment_create_and_executeResult/shipment/information/shipper_name/text()";

    public void parseXMLAndPersistShipmentToDB(XmlShipmentCreateAndExecuteResponse legacyShipmentResponse, String originEntity) {
        String msg = "LegacySoapHelper.parseXMLAndPersistShipmentToDB()";
        // A Future chain for related DB transactions, in order
        CompletableFuture<Void> svcDBExecution = CompletableFuture.runAsync(() -> {
        }, svcDBExecutor);
        try {
            log.info("Legacy Response parser");
            StringWriter responseSW = new StringWriter();
            Marshaller marshaller = JAXBContext.newInstance(XmlShipmentCreateAndExecuteResponse.class).createMarshaller();
            marshaller.marshal(legacyShipmentResponse, responseSW);
            Document doc = convertStringToDocument(responseSW.toString());

            if (null != doc) {
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xp = xpf.newXPath();
                String shipmentId = xp.evaluate(SHIPMENT_ID_TAG, doc.getDocumentElement());
                String carrierName = xp.evaluate(CARRIER_NAME_TAG, doc.getDocumentElement());
                log.info("Legacy Created ShipmentId # {} carrier # {}", shipmentId, carrierName);
                if (StringUtils.isNotEmpty(shipmentId))
                    svcDBExecution.thenRunAsync(() -> svcMessageService.saveLegacyShipmentMessage(shipmentId, originEntity, carrierName), svcDBExecutor);
            }
        } catch (Exception exp) {
            log.error("Error Occurred", exp);
        }
    }

    private Document convertStringToDocument(String xmlStr) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));
            return doc;
        } catch (Exception exp) {
            log.error("Error Occurred", exp);
        }
        return null;
    }

    public String retrieveOriginEntity(String shipmentId) {
        return svcMessageService.retrieveOriginEntity(shipmentId);
    }
}
