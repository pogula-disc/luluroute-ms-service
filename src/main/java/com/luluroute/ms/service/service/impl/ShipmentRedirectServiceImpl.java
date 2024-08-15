package com.luluroute.ms.service.service.impl;

import com.enroutecorp.ws.inbound.ShipmentCancel;
import com.enroutecorp.ws.inbound.Shipments;
import com.enroutecorp.ws.inbound.XmlShipmentCreateAndExecute;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.config.XmlJsonConfig;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.InvalidInputException;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import com.luluroute.ms.service.helper.SoapContextHelper;
import com.luluroute.ms.service.kafka.ShipmentMessageProcessor;
import com.luluroute.ms.service.service.LegacyShipmentService;
import com.luluroute.ms.service.service.ShipmentRedirectService;
import com.luluroute.ms.service.service.SvcMessageService;
import com.luluroute.ms.service.service.impl.soapwsproxy.LegacySoapShipmentServiceImpl;
import com.luluroute.ms.service.util.RequestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.luluroute.ms.service.helper.SoapContextHelper.parseInboundShipmentContent;
import static com.luluroute.ms.service.util.ShipmentConstants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShipmentRedirectServiceImpl implements ShipmentRedirectService {

    private final ShipmentMessageProcessor producerService;

    private final LegacyShipmentService legacyShipmentService;

    private final LegacySoapShipmentServiceImpl legacySoapShipmentService;

    private final SoapToRestShipmentServiceImpl soapToRestShipmentService;

    private final AppConfig appConfig;

    private final SoapContextHelper soapContextHelper;

    private final XmlJsonConfig xmlJsonConfig;

    private final SvcMessageService svcMessageService;


    /**
     * Redirects shipment message to 2.0 services based on app config values
     * @param shipmentMessage - Shipment create/cancel request
     * @return ShipmentServiceResponse - Returns the shipment create/cancel response.
     */
    @Override
    public CompletableFuture<ShipmentServiceResponse> redirectShipmentMessage(ShipmentMessage shipmentMessage) {
        String requestType = shipmentMessage.getRequestHeader().getRequestType();
        String correlationId = shipmentMessage.getMessageHeader().getMessageCorrelationId();
        if (isNonLegacyRequest(shipmentMessage, requestType)) {
            log.info("Proceeding to process shipment through LuluRoute 2.0 with messageCorrelationId: {}", correlationId);
            return producerService.processShipmentMessage(shipmentMessage);
        } else {
            log.info("Proceeding to process shipment through LuluRoute Legacy with messageCorrelationId: {}", correlationId);
            return CompletableFuture.completedFuture(legacyShipmentService.processShipmentMessage(shipmentMessage));
        }
    }

    @Override
    public Node redirectShipmentMessage(XmlShipmentCreateAndExecute inboundRequest) throws LegacySoapWebServiceException {
        Shipments xmlRequest = parseInboundShipmentContent(inboundRequest.getXml());
        MDC.put(USER_ZIP_CODE, extractAndMapZipCode(xmlRequest));

        if (is2_0SoapShipmentCreate(xmlRequest)) {
            log.info("Proceeding to process shipment through LuluRoute 2.0");
            return soapToRestShipmentService.processXmlShipmentCreateAndExecute(xmlRequest);
        } else {
            log.info("Proceeding to process shipment through LuluRoute Legacy");
            return legacySoapShipmentService.processXmlShipmentCreateAndExecute(inboundRequest);
        }
    }

    @Override
    public Node redirectShipmentCancelMessage(ShipmentCancel inboundRequest) throws LegacySoapWebServiceException {

        checkAndUpdateUserProfile();

        if (!StringUtils.isNumeric(inboundRequest.getShipmentId())) {
            log.info("Proceeding to process shipment through LuluRoute 2.0");
            return soapToRestShipmentService.processShipmentCancel();
        } else {
            log.info("Proceeding to process shipment through LuluRoute Legacy");
            return legacySoapShipmentService.processShipmentCancel(inboundRequest);
        }
    }

    private void checkAndUpdateUserProfile() {
        if (null == MDC.get(USER_ID)) {
            String originEntityId = svcMessageService.retrieveOriginEntity(MDC.get(X_SHIPMENT_CORRELATION_ID));
            log.info("Cancel originEntityId # {} ", originEntityId);

            if (org.apache.commons.lang3.StringUtils.isNotEmpty(originEntityId))
                MDC.put(USER_DC_ENTITY, originEntityId);
        }
    }

    public boolean isNonLegacyRequest(ShipmentMessage shipmentMessage, String requestType) {
        List<String> entityCodes = getOriginEntityCode(shipmentMessage);
        String orderType = "";
        if (RequestType.SHIPMENT_REQ.getValue().equals(requestType)) {
            orderType = shipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails().getOrderType();
        } else if (RequestType.CANCEL_REQ.getValue().equals(requestType)) {
            orderType = getOrderTypeByShipmentId(shipmentMessage.getMessageBody()
                    .getShipments().get(0).getShipmentHeader().getShipmentCorrelationId());
        }
        return isNonLegacyShipmentRequest(shipmentMessage, entityCodes, orderType);
    }

    public boolean isNonLegacyShipmentRequest(ShipmentMessage shipmentMessage, List<String> originEntityCodes,
                                              String orderType) {
        List<String> messageEntityCodes = getMessageEntityCodes(shipmentMessage);
        List<String> allowableEntityCodes = getAllowableEntityCodes(messageEntityCodes);

        boolean isPartiallyRouted = isPartiallyRoutedEntityCode(appConfig.getPartiallyRoutedEntityCodes(), originEntityCodes);
        boolean isValidOrder = isValidOrderType(appConfig.getRetailOrderTypes(), orderType);
        boolean isValidMessageEntity = isValidEntityCode(appConfig.getMessageEntityCodes(), messageEntityCodes);
        boolean isValidOriginEntity = isValidEntityCode(allowableEntityCodes, originEntityCodes);

        return (isPartiallyRouted && isValidOrder) || (isValidMessageEntity && isValidOriginEntity);
    }

    private List<String> getAllowableEntityCodes(List<String> messageEntityCodes) {
        if (messageEntityCodes.contains(SFS_CA)) {
            return appConfig.getOriginEntityCodesSFSCA();
        } else if (messageEntityCodes.contains(SFS_US)) {
            return appConfig.getOriginEntityCodesSFSUS();
        } else {
            return appConfig.getOriginEntityCodes();
        }
    }

    public boolean is2_0SoapShipmentCreate( Shipments xmlRequest) throws LegacySoapWebServiceException {
        boolean xmlHas2_0RoutingElement = false;
        for(String element : xmlJsonConfig.getElementsToRoute2_0()) {
            // reference_6  = OrderType
            if(xmlRequest.getShipment() != null &&
                    xmlRequest.getShipment().get(0) != null &&
                    xmlRequest.getShipment().get(0).getReference6() != null &&
                    xmlRequest.getShipment().get(0).getReference6().toLowerCase().contains((element.toLowerCase()))) {
                xmlHas2_0RoutingElement = true;
                break;
            }
        }
        return xmlHas2_0RoutingElement &&
                isValidEntityCode(appConfig.getOriginEntityCodes(), List.of(soapContextHelper.getEntityCodeFromUser()));
    }

    public static boolean isValidEntityCode(List<String> allowableEntityCodes, List<String> shipmentEntityCodes) {
        if (!shipmentEntityCodes.isEmpty()) {
            for (String entityCode : shipmentEntityCodes) {
                if (allowableEntityCodes.contains(entityCode)) {
                    return true;
                }
            }
        }
        log.info("Message Source or Shipment Origin Entity Code is not accepted by LuluRoute 2.0");
        return false;
    }

    public static boolean isPartiallyRoutedEntityCode(List<String> allowableEntityCodes, List<String> shipmentEntityCodes) {
        return isValidEntityCode(allowableEntityCodes, shipmentEntityCodes);
    }

    public static boolean isValidOrderType(List<String> allowableOrderTypes, String orderType) {
        if (StringUtils.isNotBlank(orderType) && allowableOrderTypes.contains(orderType.toUpperCase())) {
            log.info("OrderType: {} is supported by LuluRoute 2.0", orderType);
            return true;
        }
        log.info("OrderType: {} is supported by Legacy", orderType);
        return false;
    }

    public static  List<String> getMessageEntityCodes(ShipmentMessage shipmentMessage) throws InvalidInputException {
        List<String> messageEntityCodes = new ArrayList<>();
        if (validMessageSources(shipmentMessage)) {
            List<EntityRole> messageSources = shipmentMessage.getMessageHeader().getMessageSources();
            messageSources.forEach(entityRole -> messageEntityCodes.add(entityRole.getEntityCode()));
            return messageEntityCodes;
        }
        return Collections.emptyList();
    }

    public static List<String> getOriginEntityCode(ShipmentMessage shipmentMessage) throws InvalidInputException {
        List<String> originEntityCodes = new ArrayList<>();
        if (validShipments(shipmentMessage)) {
            List<ShipmentInfo> shipmentInfo = shipmentMessage.getMessageBody().getShipments();
            shipmentInfo.forEach(shipment -> originEntityCodes.add(shipment.getShipmentHeader().getOrigin().getEntityCode()));
            return originEntityCodes;
        }
        return Collections.emptyList();
    }

    private static boolean validMessageSources(ShipmentMessage shipmentMessage) {
        if (ObjectUtils.isEmpty(shipmentMessage)) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageHeader())) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageHeader().getMessageSources())) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageHeader().getMessageSources().get(0))) {
            return false;
        } else return !ObjectUtils.isEmpty(shipmentMessage.getMessageHeader().getMessageSources().get(0).getEntityCode());
    }

    private static boolean validShipments(ShipmentMessage shipmentMessage) {
        if (ObjectUtils.isEmpty(shipmentMessage)) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageBody())) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageBody().getShipments())) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageBody().getShipments().get(0))) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader())) {
            return false;
        } else if (ObjectUtils.isEmpty(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getOrigin())) {
            return false;
        } else return !ObjectUtils.isEmpty(shipmentMessage.getMessageBody().getShipments().get(0).getShipmentHeader().getOrigin().getEntityCode());
    }

    private String extractAndMapZipCode(Shipments xmlRequest) {
        return  StringUtils.isNotEmpty(xmlRequest.getShipment().get(0).getFromZip()) ?
                xmlRequest.getShipment().get(0).getFromZip().replaceAll("\\s+","") :
                xmlRequest.getShipment().get(0).getFromZip();
    }

    public String getOrderTypeByShipmentId(String shipmentCorrelationId) {
        ShipmentMessage shipmentMessage = svcMessageService.getShipmentMessageByCorrelationId(shipmentCorrelationId);
        if (shipmentMessage != null
                && shipmentMessage.getMessageBody().getShipments() != null
                && shipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails() != null)
            return shipmentMessage.getMessageBody().getShipments().get(0).getOrderDetails().getOrderType();
        return "";
    }

}
