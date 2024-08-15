package com.luluroute.ms.service.mapper;

import com.enroutecorp.ws.inbound.Shipments;
import com.logistics.luluroute.domain.Shipment.Message.MessageBodyInfo;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentHeader;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.*;
import com.logistics.luluroute.domain.Shipment.Shared.StatusItem;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.luluroute.ms.service.util.LegacyXmlDocumentConstants.DOCUMENT_TIME_ZONE_UTC;
import static com.luluroute.ms.service.util.MultiCarrierAttributesUtil.PIPE_DELIMITED;
import static com.luluroute.ms.service.util.ShipmentConstants.STORE_NUMBER_LENGTH_WITH_ZEROS;
import static com.luluroute.ms.service.util.ShipmentConstants.X_SHIPMENT_CORRELATION_ID;
import static java.lang.Math.max;
import static java.time.ZoneId.of;
import static java.time.format.DateTimeFormatter.ofPattern;

@Mapper(componentModel = "spring")
@Slf4j
public abstract class XmlJsonShipmentComponentMapper {

    /** Map all fields that can be sourced exclusively from the XML */
    public ShipmentMessage buildFieldsFromXml(
            Shipments.Shipment xmlShipment, String xmlTimeZone, EntityPayload originEntity) {
        log.debug("Mapping JSON fields found directly in XML");
        return ShipmentMessage.builder()
                .MessageBody(MessageBodyInfo.builder()
                        .shipments(List.of(ShipmentInfo.builder()
                                .shipmentHeader(buildShipmentHeader(xmlShipment, originEntity))
                                .shipmentPieces(List.of(buildShipmentPieceInfo(xmlShipment)))
                                .orderDetails(addRemainingOrderDetailsFields(xmlShipment, mapOrderDetails(xmlShipment)))
                                .transitDetails(addRemainingTransitDetailsFields(
                                        xmlShipment, xmlTimeZone, mapTransitDetails(xmlShipment)))
                                .shipmentStatus(StatusItem.builder().build())
                                .build()))
                        .build())
                .build();
    }

    private ShipmentHeader buildShipmentHeader(Shipments.Shipment xmlShipment, EntityPayload entityPayload) {
        ShipmentHeader shipmentHeader = ShipmentHeader.builder()
                .shipmentCorrelationId(MDC.get(X_SHIPMENT_CORRELATION_ID))
                .origin(mapAddressFrom(xmlShipment))
                .destination(mapAddressTo(xmlShipment))
                .returnLocation(mapAddressReturn(xmlShipment))
                .build();

        // AddressCategory split
        shipmentHeader.getDestination().getAddressTo().setAddressCategory(
                    StringUtils.isNotEmpty(xmlShipment.getAddressCategory()) ?
                  xmlShipment.getAddressCategory().split(PIPE_DELIMITED, -1) : null);

        log.debug(" AddressCategory {} ", shipmentHeader.getDestination().getAddressTo().getAddressCategory());

        shipmentHeader.getOrigin().setEntityCode(entityPayload.getEntityCode());
        addLeadingZerosToStoreNumber(shipmentHeader.getDestination());
        shipmentHeader.getReturnLocation().setEntityCode(entityPayload.getEntityCode());
        return shipmentHeader;
    }

    private ShipmentPieceInfo buildShipmentPieceInfo(Shipments.Shipment xmlShipment) {
        ShipmentPieceInfo shipmentPieceInfo = mapShipmentPieces(xmlShipment);
        shipmentPieceInfo.setSequence(1);
        shipmentPieceInfo.setTotalSequence(1);
        shipmentPieceInfo.setCartonsDetails(buildCartonsDetails(xmlShipment));
        return shipmentPieceInfo;
    }

    private List<CartonInfo> buildCartonsDetails(Shipments.Shipment xmlShipment) {
        List<CartonInfo> cartonsDetails = mapCartonDetails(getXmlShipmentItems(xmlShipment));
        AtomicInteger sequenceCount = new AtomicInteger(1);
        final int total = cartonsDetails.size();
        cartonsDetails.forEach(cartonInfo -> {
            cartonInfo.getWeightDetails().setUom(xmlShipment.getWeightType());
            cartonInfo.setItemSequence(sequenceCount.getAndAdd(1));
            cartonInfo.setItemSequenceTotal(total);
            if(cartonInfo.getQuantity() == 0) {
                cartonInfo.setQuantity(1);
            }
        });
        return cartonsDetails;
    }

    private OrderInfo addRemainingOrderDetailsFields(Shipments.Shipment xmlShipment, OrderInfo orderInfo) {
        var xmlShipmentItems = getXmlShipmentItems(xmlShipment);

        orderInfo.setShipmentContents(getConcatenatedItemsContents(xmlShipmentItems));
        orderInfo.getDeclaredValueDetails().setValue(getTotalDeclaredValueOfItems(xmlShipmentItems));
        orderInfo.getDeclaredValueDetails().setCurrency(getAnyItemCurrency(xmlShipmentItems));

        return orderInfo;
    }

    private TransitInfo addRemainingTransitDetailsFields(
            Shipments.Shipment xmlShipment, String xmlTimeZone, TransitInfo transitInfo) {

        String xmlPlannedShipDate = StringUtils.isEmpty(xmlShipment.getShipDate()) ?
                xmlShipment.getPlannedShipDate() : xmlShipment.getShipDate();
        String jsonTimeZone = mapJsonTimeZone(xmlTimeZone);
        long plannedShipDate = mapEpochPSDDate(xmlPlannedShipDate, jsonTimeZone);
        long plannedDeliveryDate = mapEpochDate(xmlShipment.getPlannedDeliveryDate(), jsonTimeZone);
        transitInfo.setDateDetails(ShipmentDatesInfo.builder()
                .plannedDeliveryDate(plannedDeliveryDate)
                .plannedShipDate(plannedShipDate)
                .estimatedDeliveryDate(mapEstimatedDeliveryDate(xmlShipment.getEstimatedDeliveryDate(), jsonTimeZone))
                .serviceDateCommitmentMin(convertToLong(xmlShipment.getServiceDateCommitmentMin()))
                .serviceDateCommitmentMax(convertToLong(xmlShipment.getServiceDateCommitmentMax()))
                .build());

        if(StringUtils.isNumeric(xmlShipment.getReference8())) { // If shipping to store
            transitInfo.getDateDetails().setInStoreDate(plannedDeliveryDate);
        }
        if(StringUtils.isEmpty(transitInfo.getFrontBackOfHouse())) {
            transitInfo.setFrontBackOfHouse("B");
        }
        if(StringUtils.isEmpty(xmlShipment.getLabelType())) {
            if(transitInfo.getLabelDetails() == null) {
                transitInfo.setLabelDetails(ShipmentLabelInfo.builder().build());
            }
            transitInfo.getLabelDetails().setFormat("ZPL");
        }
        return transitInfo;
    }

    private Long convertToLong(String value) {
       return StringUtils.isNotEmpty(value) ? Long.valueOf(value) : null;
    }

    @Mapping(target = "addressFrom.contact", source = "fromDesc1")
    @Mapping(target = "addressFrom.description1", source = "fromDesc3")
    @Mapping(target = "addressFrom.description2", source = "fromDesc4")
    @Mapping(target = "addressFrom.description3", source = "fromDesc5")
    @Mapping(target = "addressFrom.city", source = "fromCity")
    @Mapping(target = "addressFrom.state", source = "fromState")
    @Mapping(target = "addressFrom.zipCode", source = "fromZip")
    @Mapping(target = "addressFrom.country", source = "fromCountry")
    @Mapping(target = "addressFrom.contactPhone", source = "fromPhone")
    abstract OriginInfo mapAddressFrom(Shipments.Shipment shipment);

    @Mapping(target = "addressTo.contact", source = "toDesc1")
    @Mapping(target = "addressTo.description1", source = "toDesc3")
    @Mapping(target = "addressTo.description2", source = "toDesc4")
    @Mapping(target = "addressTo.description3", source = "toDesc5")
    @Mapping(target = "addressTo.city", source = "toCity")
    @Mapping(target = "addressTo.state", source = "toState")
    @Mapping(target = "addressTo.zipCode", source = "toZip")
    @Mapping(target = "addressTo.country", source = "toCountry")
    @Mapping(target = "addressTo.contactPhone", source = "toPhone")
    @Mapping(target = "entityCode", source = "reference8")
    abstract DestinationInfo mapAddressTo(Shipments.Shipment shipment);

    @Mapping(target = "returnAddress.contact", source = "returnDesc1")
    @Mapping(target = "returnAddress.description1", source = "returnDesc3")
    @Mapping(target = "returnAddress.description2", source = "returnDesc4")
    @Mapping(target = "returnAddress.description3", source = "returnDesc5")
    @Mapping(target = "returnAddress.city", source = "returnCity")
    @Mapping(target = "returnAddress.state", source = "returnState")
    @Mapping(target = "returnAddress.zipCode", source = "returnZip")
    @Mapping(target = "returnAddress.country", source = "returnCountry")
    @Mapping(target = "returnAddress.contactPhone", source = "returnPhone")
    abstract ReturnInfo mapAddressReturn(Shipments.Shipment shipment);

    @Mapping(target = "dimensionDetails.length", source = "length")
    @Mapping(target = "dimensionDetails.width", source = "width")
    @Mapping(target = "dimensionDetails.height", source = "height")
    @Mapping(target = "dimensionDetails.uom", constant = "IN")
    @Mapping(target = "weightDetails.value", source = "weight")
    @Mapping(target = "weightDetails.uom", source = "weightType")
    abstract ShipmentPieceInfo mapShipmentPieces(Shipments.Shipment shipment);

    @Mapping(target = "orderId", source = "reference2")
    @Mapping(target = "tclpnid", source = "reference1")
    @Mapping(target = "lPNTCOrderId", source = "reference2")
    @Mapping(target = "laneName", source = "reference3")
    @Mapping(target = "shipVia", source = "reference4")
    @Mapping(target = "orderType", source = "reference6")
    @Mapping(target = "referenceLPN", source = "reference9")
    @Mapping(target = "declaredValueDetails.value", source = "declaredValue")
    abstract OrderInfo mapOrderDetails(Shipments.Shipment shipment);

    @Mapping(target = "region", source = "reference5")
    @Mapping(target = "frontBackOfHouse", source = "reference10")
    @Mapping(target = "labelDetails.format", source = "labelType")
    abstract TransitInfo mapTransitDetails(Shipments.Shipment shipment);


    @Mapping(target = "itemCode", source = "itemcode")
    @Mapping(target = "description", source = "description", conditionExpression ="java(org.apache.commons.lang3.StringUtils.isNotEmpty(item.getDescription()))", defaultValue = "Sports Apparel and Accessories")
    @Mapping(target = "tradeDetails.hsTariffNumber", source = "hstariffnumber")
    @Mapping(target = "tradeDetails.countryOfOriginCode", source = "countryoforigin")
    @Mapping(target = "tradeDetails.itemsContents", source = "contents")
    @Mapping(target = "tradeDetails.declaredValueDetails.currency", source = "currency")
    @Mapping(target = "tradeDetails.declaredValueDetails.value", source = "declaredValue")
    @Mapping(target = "itemValue.value", source = "value")
    @Mapping(target = "itemValue.currency", source = "currency")
    @Mapping(target = "tradeDetails.manufacturerID", source = "extendeddesc")
    @Mapping(target = "weightDetails.value", source = "weight")
    abstract CartonInfo mapCartonDetail(Shipments.Shipment.Pieces.Piece.Items.Item item);

    abstract List<CartonInfo> mapCartonDetails(List<Shipments.Shipment.Pieces.Piece.Items.Item> items);


    boolean stringToBoolean(String stringInput) {
        return "TRUE".equalsIgnoreCase(stringInput) || "1".equals(stringInput);
    }

    String stringToValueOrNull(String stringInput) {
        return StringUtils.isEmpty(stringInput) ? null : stringInput;
    }

    String mapContact(String desc1, String desc2) {
        return String.format("%s %s", desc1, desc2).trim();
    }

    private static String mapJsonTimeZone(String xmlTimeZone) {
        if(StringUtils.equals(DOCUMENT_TIME_ZONE_UTC, xmlTimeZone))
            return "UTC";
        return "America/Los_Angeles";
    }

    public static long mapEpochPSDDate(String date, String jsonTimeZone) {
        if(StringUtils.isEmpty(date))
            return 0;

        LocalDateTime localDateTime;
        if(date.contains(" ")) {
            localDateTime = LocalDateTime.parse(date, ofPattern("M/d/yyyy H:mm:ss"));
        } else {
            localDateTime = LocalDate.parse(date, ofPattern("M/d/yyyy")).atTime(LocalTime.now(of(jsonTimeZone)));
        }

        return localDateTime.atZone(of(jsonTimeZone)).toEpochSecond();
    }


    public static long mapEpochDate(String date, String jsonTimeZone) {
        if(StringUtils.isEmpty(date))
            return 0;

        LocalDateTime localDateTime;
        if(date.contains(" ")) {
            localDateTime = LocalDateTime.parse(date, ofPattern("M/d/yyyy H:mm:ss"));
        } else {
            localDateTime = LocalDate.parse(date, ofPattern("M/d/yyyy")).atTime(16,0);
        }

        return localDateTime.atZone(of(jsonTimeZone)).toEpochSecond();
    }

    public static long mapEstimatedDeliveryDate(String date, String jsonTimeZone) {
        if(StringUtils.isEmpty(date))
            return 0;
        LocalDateTime localDateTime = LocalDateTime.parse(date, ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

        return localDateTime.atZone(of(jsonTimeZone)).toEpochSecond();
    }

    public static void addLeadingZerosToStoreNumber(DestinationInfo destinationInfo) {
        String destinationEntity = destinationInfo.getEntityCode();
        if(StringUtils.isNumeric(destinationEntity)) {
            destinationInfo.setEntityCode(StringUtils.leftPad(destinationEntity, STORE_NUMBER_LENGTH_WITH_ZEROS, '0'));
        }
    }

    public List<Shipments.Shipment.Pieces.Piece.Items.Item> getXmlShipmentItems(Shipments.Shipment xmlShipment) {
        if (xmlShipment.getPieces() != null
                && xmlShipment.getPieces().getPiece() != null
                && xmlShipment.getPieces().getPiece().getItems() != null) {
            return xmlShipment.getPieces().getPiece().getItems().getItem();
        }
        return List.of();
    }

    public static double getTotalDeclaredValueOfItems(List<Shipments.Shipment.Pieces.Piece.Items.Item> xmlShipmentItems) {
        return xmlShipmentItems.stream()
                .map(item -> {
                    BigDecimal declaredValue = item.getDeclaredValue() == null ? BigDecimal.ZERO : item.getDeclaredValue();
                    return declaredValue.multiply(BigDecimal.valueOf(max(item.getQuantity(), 1)));
                }).reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();
    }

    private static String getConcatenatedItemsContents(List<Shipments.Shipment.Pieces.Piece.Items.Item> xmlShipmentItems) {
        return xmlShipmentItems.stream()
                .map(Shipments.Shipment.Pieces.Piece.Items.Item::getContents)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private static String getAnyItemCurrency(List<Shipments.Shipment.Pieces.Piece.Items.Item> xmlShipmentItems) {
        var anyItem = xmlShipmentItems.stream()
                .findAny().orElse(null);
        return anyItem == null ? null : anyItem.getCurrency();
    }

}
