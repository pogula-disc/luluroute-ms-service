package com.luluroute.ms.service.service.impl;

import com.enroutecorp.ws.inbound.Shipments;
import com.enroutecorp.ws.inbound.XmlShipmentCreateAndExecute;
import com.enroutecorp.ws.inbound.content.ShipmentCancelContent;
import com.enroutecorp.ws.inbound.content.ShipmentSuccess;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.service.config.XmlJsonConfig;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.ShipmentCancellationException;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import com.luluroute.ms.service.helper.SoapContextHelper;
import com.luluroute.ms.service.kafka.ShipmentMessageProcessor;
import com.luluroute.ms.service.mapper.JsonXmlShipmentResponseMapper;
import com.luluroute.ms.service.mapper.XmlJsonShipmentRequestMapper;
import com.luluroute.ms.service.service.RedisRehydrateService;
import com.luluroute.ms.service.service.SvcMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static com.luluroute.ms.service.helper.SoapContextHelper.*;
import static com.luluroute.ms.service.util.ShipmentConstants.*;
import static com.luluroute.ms.service.validator.RestToSoapValidator.checkErrors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoapToRestShipmentServiceImpl {

    private final ShipmentMessageProcessor producerService;
    private final XmlJsonShipmentRequestMapper shipmentRequestMapper;
    private final JsonXmlShipmentResponseMapper shipmentResponseMapper;
    private final RedisRehydrateService redisRehydrateService;
    private final SoapContextHelper soapContextHelper;
    private final XmlJsonConfig xmlJsonConfig;
    private final SvcMessageService svcMessageService;

    public Node processXmlShipmentCreateAndExecute(Shipments xmlRequest)
            throws LegacySoapWebServiceException {
        Instant start = Instant.now();

        EntityPayload originEntity = redisRehydrateService
                .getEntityByCode(soapContextHelper.getEntityCodeFromUser());

        if(null == originEntity)
            originEntity = redisRehydrateService
                    .getEntityByCode(soapContextHelper.getSoapUserFromZipCode(xmlRequest.getShipment().get(0).getFromZip()));

        ShipmentMessage jsonRequest = shipmentRequestMapper
                .mapShipmentCreateToJson(xmlRequest.getShipment().get(0), xmlRequest.getDocumentTimeZone(), originEntity, start);

        ShipmentServiceResponse jsonResponse = processShipmentMessage(jsonRequest);

        Instant end = Instant.now();
        ShipmentSuccess xmlResponse = shipmentResponseMapper
                .mapShipmentCreateToXml(jsonResponse.getShipmentMessage(), start, end, xmlJsonConfig);
        return buildSuccessResponse(xmlResponse);
    }

    public Node processShipmentCancel() throws LegacySoapWebServiceException {
        Instant start = Instant.now();
        try {
            EntityPayload originEntity = redisRehydrateService
                    .getEntityByCode(soapContextHelper.getEntityCodeFromUser());
            ShipmentMessage jsonRequest = shipmentRequestMapper
                    .mapShipmentCancelToJson(originEntity, start);

            processShipmentMessage(jsonRequest);
        }  catch (ShipmentCancellationException e) {
            if(StringUtils.equals(e.getMessage(), CANCEL_RESPONSE_ALREADY_CANCELLED)) {
                log.warn(CANCEL_RESPONSE_ALREADY_CANCELLED);
            } else {
                throw new LegacySoapWebServiceException(e.getMessage(), e, null, false);
            }
        }

        ShipmentCancelContent shipmentCancel = shipmentResponseMapper
                .mapShipmentCancelToXml();
        return buildCancelResponse(shipmentCancel);
    }

    private ShipmentServiceResponse processShipmentMessage(ShipmentMessage jsonRequest) throws LegacySoapWebServiceException {
        ShipmentServiceResponse jsonResponse;
        try {
            jsonResponse = producerService.processShipmentMessage(jsonRequest).get();
        } catch (InterruptedException | ExecutionException e) { // Ignore linting -- wrapped by LegacySoapWebServiceException
            if (e.getCause() instanceof ShipmentServiceException sse) {
                throw sse;
            }
            throw new LegacySoapWebServiceException("Unexpected exception while waiting for 2.0 response", e, null, true);
        }
        checkErrors(jsonResponse);
        return jsonResponse;
    }
}
