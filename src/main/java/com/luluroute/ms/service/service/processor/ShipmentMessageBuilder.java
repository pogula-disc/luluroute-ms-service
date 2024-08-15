package com.luluroute.ms.service.service.processor;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.*;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;
import com.luluroute.ms.service.dto.ServiceLocationDto;
import com.luluroute.ms.service.model.ServiceDetail;
import com.luluroute.ms.service.util.HashKeyGenerator;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ShipmentMessageBuilder {

    public ServiceDetail buildServiceDetails(ShipmentMessage shipmentMessage) {
        List<ShipmentInfo> shipmentInfos = shipmentMessage.getMessageBody().getShipments();
        ShipmentInfo shipmentInfo = shipmentInfos.get(0);
        TransitInfo transitInfo = shipmentInfo.getTransitDetails();
        DestinationInfo destination = shipmentInfo.getShipmentHeader().getDestination();
        ShipmentDatesInfo datesInfo = transitInfo.getDateDetails();
        Date estDate = datesInfo.getEstimatedDeliveryDate() > 0 ? new Date(datesInfo.getEstimatedDeliveryDate() * 1000) : null;
        Date plannedDelivery = datesInfo.getPlannedDeliveryDate() > 0
                ? new Date(datesInfo.getPlannedDeliveryDate() * 1000) : null;
        Date deliveredDate = datesInfo.getDeliveredDate() > 0 ? new Date(datesInfo.getDeliveredDate() * 1000) : null;
        Date plannedShip = datesInfo.getPlannedShipDate() > 0 ? new Date(datesInfo.getPlannedShipDate() * 1000) : null;
        Date shippedDate = datesInfo.getShippedDate() > 0 ? new Date(datesInfo.getShippedDate() * 1000) : null;
        RequestInfo requestInfo = shipmentMessage.getRequestHeader();
        Date reqDate = requestInfo.getRequestDate() > 0 ? new Date(requestInfo.getRequestDate() * 1000) : null;
        OrderInfo orderInfo = shipmentInfo.getOrderDetails();

        return ServiceDetail.builder().estimatedDeliveryDate(estDate)
                .correlationId(shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                .svcDetailType(Integer.valueOf(shipmentMessage.getRequestHeader().getRequestType()))
                .transitDays(datesInfo.getTransitDays()).reqAccount(requestInfo.getRequestId())
                .reqDate(reqDate).reqEntityCode(
                        shipmentMessage.getMessageHeader().getMessageSources().get(0).getEntityCode())
                .shippedDate(shippedDate).deliveryDate(deliveredDate).plannedShipDate(plannedShip)
                .plannedDeliveryDate(plannedDelivery).dstEntityCode(destination.getEntityCode())
                .trackingNo(transitInfo.getTrackingNo())
                .transitMode(transitInfo.getTransitMode())
                .masterBol(transitInfo.getMasterBOL())
                .routeRuleCode(transitInfo.getRouteRuleCode())
                .routeOverrideCode(transitInfo.getRouteOverrideCode())
//                    .carrierCode(String.valueOf(carrierInfo.getCarrierCode()))
                .orderId(orderInfo.getOrderId()).orderType(orderInfo.getOrderType())
                .orderOrigin(orderInfo.getOrderOrigin()).orderDate(new Date(orderInfo.getOrderCreatedDate()))
                .lpn(orderInfo.getReferenceLPN())
                .lstSvcStatus("NEW").lstSvcStatusDate(new Date()).lstSvcStatusBy("System")
                .integration(orderInfo.getIntegration())
                .build();
    }

    public ServiceLocationDto buildSourceLocation(OriginInfo originInfo) {
        LocationItem locationItem = originInfo.getAddressFrom();
        return ServiceLocationDto.builder().locationCode(originInfo.getEntityCode())
                .description(locationItem.getDescription1())
                .desc1(locationItem.getDescription1())
                .desc2(locationItem.getDescription2())
                .desc3(locationItem.getDescription3())
                .desc4(locationItem.getDescription4()).city(locationItem.getCity())
                .state(locationItem.getState()).country(locationItem.getCountry())
                .zip(locationItem.getZipCode())
                .hashKey(HashKeyGenerator.generateHashKey(locationItem))
                .build();
    }

    public ServiceLocationDto buildDestinationLocation(DestinationInfo destinationInfo) {
        LocationItem locationItem = destinationInfo.getAddressTo();
        return ServiceLocationDto.builder().locationCode(destinationInfo.getEntityCode())
                .description(locationItem.getDescription1())
                .desc1(locationItem.getDescription1())
                .desc2(locationItem.getDescription2())
                .desc3(locationItem.getDescription3())
                .desc4(locationItem.getDescription4()).city(locationItem.getCity())
                .state(locationItem.getState()).country(locationItem.getCountry())
                .zip(locationItem.getZipCode())
                .hashKey(HashKeyGenerator.generateHashKey(locationItem))
                .build();
    }

}
