package com.luluroute.ms.service.mapper;

import com.enroutecorp.ws.inbound.content.ShipmentCancelContent;
import com.enroutecorp.ws.inbound.content.ShipmentSuccess;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentDatesInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.RateShopResponse;
import com.luluroute.ms.service.config.XmlJsonConfig;
import com.luluroute.ms.service.util.ShipmentCorrelationIdUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

import static com.luluroute.ms.service.util.LegacyXmlDocumentConstants.DOCUMENT_TIME_ZONE_PACIFIC;
import static com.luluroute.ms.service.util.ShipmentConstants.SHIPMENT_CANCELED_TEXT_SOAP;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneId.of;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.ofPattern;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class JsonXmlShipmentResponseMapper {
    //dd/mm/yyyy HH:MM:SS
    // Ex. 08/08/2024 17:00:00
    String CUT_OFF_TIME = "%s %s:%s:%s";

    public ShipmentSuccess mapShipmentCreateToXml(
            ShipmentMessage jsonShipment, Instant start, Instant end, XmlJsonConfig xmlJsonConfig) {
        log.debug("Beginning inbound JSON Shipment mapping to XML Shipment response");
        ShipmentSuccess response = new ShipmentSuccess();
        var shipment = mapShipment(jsonShipment);
        response.setShipment(shipment);
        shipment.setPerformance(mapPerformance(start, end));
        shipment.setInformation(mapInformation(jsonShipment.getMessageBody().getShipments().get(0), xmlJsonConfig));
        shipment.setDocumentTimeZone(DOCUMENT_TIME_ZONE_PACIFIC);
        return response;
    }

    public ShipmentCancelContent mapShipmentCancelToXml() {
        log.debug("Beginning inbound JSON Cancel mapping to XML Cancel response");
        ShipmentCancelContent xmlResponse = new ShipmentCancelContent();
        ShipmentCancelContent.Shipment shipment = new ShipmentCancelContent.Shipment();
        shipment.setStatus(Byte.parseByte("1"));
        shipment.setMessage(SHIPMENT_CANCELED_TEXT_SOAP);
        xmlResponse.setShipment(shipment);
        return xmlResponse;
    }

    private ShipmentSuccess.Shipment.Performance mapPerformance(Instant start, Instant end) {
        var performance = new ShipmentSuccess.Shipment.Performance();
        performance.setStartTime(xmlDateFrom(start.getEpochSecond()));
        performance.setTotalTime(xmlDurationBetween(start.getEpochSecond(), end.getEpochSecond()));
        return performance;
    }

    private ShipmentSuccess.Shipment.Information mapInformation(
            ShipmentInfo jsonShipmentInfo, XmlJsonConfig xmlJsonConfig) {
        var information = mapInformation(jsonShipmentInfo);

        ShipmentDatesInfo shipmentDatesInfo = jsonShipmentInfo.getTransitDetails().getDateDetails();
        information.setPlannedShipDate(mapUtcDate(shipmentDatesInfo.getPlannedShipDate()));
        information.setPlannedDeliveryDate(mapUtcDate(shipmentDatesInfo.getPlannedDeliveryDate()));
        information.setId(ShipmentCorrelationIdUtil.uuidToUuid64(jsonShipmentInfo.getShipmentHeader().getShipmentCorrelationId()));
        log.info("Transformed UUID from {} to {} ",jsonShipmentInfo.getShipmentHeader().getShipmentCorrelationId(),information.getId());

        RateShopResponse rate = jsonShipmentInfo.getRateShopResponses().get(0);
        information.setCost(BigDecimal.valueOf(rate.getCost()));
        information.setBaseRate(BigDecimal.valueOf(rate.getCost()));
        information.setCutOffDate(
                String.format(CUT_OFF_TIME,
                        mapUtcDateOnly(shipmentDatesInfo.getPlannedShipDate()),
                        jsonShipmentInfo.getTransitDetails().getDateDetails().getCutOffTimeHH(),
                        jsonShipmentInfo.getTransitDetails().getDateDetails().getCutOffTimeMM(),
                        "00"
                        ));

        information.setShipperId(xmlJsonConfig.getCarrierIdsByJsonCode()
                .get(jsonShipmentInfo.getShipmentHeader().getCarrier().getCarrierCode()));
        return information;
    }

    @Mapping(target = "status", constant = "1")
    abstract ShipmentSuccess.Shipment mapShipment(ShipmentMessage shipment);

    @Mapping(target = "shipperName", source = "shipmentHeader.carrier.carrierName")
    @Mapping(target = "shipperId", source = "shipmentHeader.carrier.carrierCode")
    @Mapping(target = "serviceCode", source = "transitDetails.transitMode")
    @Mapping(target = "trackingId", source = "transitDetails.trackingNo")
    @Mapping(target = "accountNo", source = "orderDetails.billingDetails.billTo")
    @Mapping(target = "carrierBarcode", source = "transitDetails.altTrackingNo")
    @Mapping(target = "pieces.piece.trackingId", source = "transitDetails.trackingNo")
    @Mapping(target = "pieces.piece.labels.label.labelimage", source = "transitDetails.labelDetails.label")
    @Mapping(target = "pieces.piece.labels.label.labeltype", source = "transitDetails.labelDetails.format")
    abstract ShipmentSuccess.Shipment.Information mapInformation(ShipmentInfo shipmentInfo);



    private static String mapUtcDate(long date) {
        DateTimeFormatter formatter = ofPattern("MM/dd/yyyy HH:mm:ss");
        return ofInstant(ofEpochSecond(date), of("America/Los_Angeles")).format(formatter);
    }

    private static String mapUtcDateOnly(long date) {
        DateTimeFormatter formatter = ofPattern("MM/dd/yyyy");
        return ofInstant(ofEpochSecond(date), of("America/Los_Angeles")).format(formatter);
    }

    @SneakyThrows
    private static XMLGregorianCalendar xmlDateFrom(long date) {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(
                        GregorianCalendar.from(ofInstant(ofEpochSecond(date), UTC)));
    }

    @SneakyThrows
    private static Duration xmlDurationBetween(long start, long end) {
        return DatatypeFactory.newInstance().newDurationDayTime(true, DatatypeConstants.FIELD_UNDEFINED,
                DatatypeConstants.FIELD_UNDEFINED, DatatypeConstants.FIELD_UNDEFINED, (int) (end - start) );
    }
}
