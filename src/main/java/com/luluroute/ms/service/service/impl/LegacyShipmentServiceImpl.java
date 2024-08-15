package com.luluroute.ms.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentInfo;
import com.logistics.luluroute.domain.Shipment.Service.ShipmentLabelInfo;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.logistics.luluroute.domain.Shipment.Shared.ResponseItem;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.exception.InvalidInputException;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.helper.ShipmentArtifactHelper;
import com.luluroute.ms.service.kafka.KafkaArtifactProducer;
import com.luluroute.ms.service.service.LegacyShipmentService;
import com.luluroute.ms.service.util.AppLogUtil;
import com.luluroute.ms.service.util.ShipmentCorrelationIdUtil;
import com.luluroute.ms.service.validator.ShipmentValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.luluroute.ms.service.service.impl.ShipmentRedirectServiceImpl.getMessageEntityCodes;
import static com.luluroute.ms.service.service.impl.ShipmentRedirectServiceImpl.isValidEntityCode;
import static com.luluroute.ms.service.util.MultiCarrierAttributesUtil.removeMultiCarrierAttributes;
import static com.luluroute.ms.service.util.RequestType.SHIPMENT_REQ;
import static com.luluroute.ms.service.util.ShipmentConstants.*;

@Service
@Slf4j
public class LegacyShipmentServiceImpl extends BaseShipmentService implements LegacyShipmentService {

    private RestTemplate restTemplate;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaArtifactProducer kafkaArtifactProducer;

    @Autowired
    private ShipmentArtifactHelper artifactHelper;

    @Value("${lulu-route.label.enabled}")
    private boolean legacyLabelsEnabled;

    public LegacyShipmentServiceImpl(RestTemplate restTemplate, AppConfig appConfig, ObjectMapper objectMapper) {
        super(appConfig, objectMapper);
        this.restTemplate = restTemplate;
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    public ShipmentServiceResponse processShipmentMessage(ShipmentMessage shipmentMessage) {
        log.info("Entering into LegacyShipmentProcessor - processShipmentMessage");
        ShipmentMessage respShipmentMsg;
        try {
            ShipmentCorrelationIdUtil.populateShipmentCorrelationId(shipmentMessage);
            log.info("LegacyShipmentProcessor TraceId: {}", AppLogUtil.getCurrentTraceId());
            log.info("Request received in ISL - request: {}", getJsonString(shipmentMessage));

            // Load respective validator.
            ShipmentValidator validator = this.getShipmentValidator(shipmentMessage);
            long start = System.currentTimeMillis();
            // Checking if payload is valid
            if (validator.isPayloadValid(shipmentMessage)) {

                if(!appConfig.isEnableMultiCarrierAttributesForLegacyJson())
                    removeMultiCarrierAttributes(shipmentMessage);

                // Checking if performance attributes present in payload.
                if (hasPerformanceAttributes(shipmentMessage)) {
                    respShipmentMsg = getMockedShipmentMessage();
                    log.info("Time taken for mocked response: {} ms", System.currentTimeMillis() - start);
                } else {
                    try {
                        respShipmentMsg = performLegacyAPICall(shipmentMessage);
                        log.info("LegacyShipmentServiceImpl : Time taken for Legacy API Response: {} ms", System.currentTimeMillis() - start);
                        ResponseItem responseItem = respShipmentMsg.getRequestHeader().getResponse();
                        if (Objects.nonNull(responseItem)) {
                            responseItem.setResponseDate(System.currentTimeMillis() / 1000);
                        }
                    } catch (ResourceAccessException resourceEx) {
                        log.error("In LegacyShipmentProcessor - request timeout {} ms ", System.currentTimeMillis() - start);
                        log.error("In LegacyShipmentProcessor - Unable to reach Legacy Shipment API", resourceEx);
                        throw resourceEx;
                    } catch (HttpStatusCodeException e) {
                        log.error("HttpStatusCodeException RawStatusCode={}, getResponseBodyAsString={}",
                                e.getRawStatusCode(), e.getResponseBodyAsString(), e);
                        throw e;
                    } catch (IllegalArgumentException exception) {
                        log.error("In LegacyShipmentProcessor IllegalArgumentException occurred.");
                        throw new InvalidInputException(exception.getMessage(), exception);
                    }
                }
            } else {
                log.error("Shipment Request payload has some validation failures: {}",
                        shipmentMessage.getRequestHeader().getResponse().getExtended().toString());
                return ShipmentServiceResponse.builder().success(false).shipmentMessage(shipmentMessage).build();
            }
        } catch (IOException ex) {
            log.error("IOException occurred while processing request payload", ex);
            throw new ShipmentServiceException(ex.getMessage(), ex);
        }
        if (legacyLabelsEnabled) {
            publishLegacyLabelAsync(respShipmentMsg, System.currentTimeMillis());
        }
        return ShipmentServiceResponse.builder().success(true).shipmentMessage(respShipmentMsg).build();
    }

    public ShipmentMessage performLegacyAPICall(ShipmentMessage shipmentMessage) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(APP_HEADER, APP_VALUE);
        HttpEntity<ShipmentMessage> reqEntity = new HttpEntity<>(shipmentMessage, headers);
        ShipmentMessage respShipmentMsg;
        try {
            String respMessage = null;
            long start = System.currentTimeMillis();
            if (isValidEntityCode(appConfig.getDcEntityCodes(), getMessageEntityCodes(shipmentMessage))) {
                log.info("DC Order");
                respMessage = restTemplate
                        .postForObject(appConfig.getServiceUrl(), reqEntity, String.class);
                log.info("LegacyShipmentServiceImpl : Legacy API call DC: {} ms", System.currentTimeMillis() - start);
            } else {
                log.info("SFS Order");
                respMessage = restTemplate
                        .postForObject(appConfig.getServiceUrl(), reqEntity, String.class);
                log.info("LegacyShipmentServiceImpl : Legacy API call SFS: {} ms", System.currentTimeMillis() - start);
            }

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            respShipmentMsg = objectMapper.readValue(respMessage, ShipmentMessage.class);
            log.info("Response from legacyShipmentService - response: {}", getJsonString(respShipmentMsg));
        } catch (JsonProcessingException exception) {
            log.error("In LegacyShipmentProcessor - Unable to process Legacy shipment response. {}", ExceptionUtils.getStackTrace(exception));
            throw exception;
        }

        return respShipmentMsg;
    }

    public void publishLegacyLabelAsync(ShipmentMessage respShipmentMsg, long startTime) {
        CompletableFuture.runAsync(() -> {
            if (successShipReqApiCall(respShipmentMsg) && validLegacyEntityForLabel(respShipmentMsg)) {
                log.info("Async Legacy Shipment Label publish has started");
                String srcEntityCode = respShipmentMsg.getMessageHeader().getMessageSources().get(0).getEntityCode();
                publishLabelArtifact(respShipmentMsg, startTime, srcEntityCode);
            }
        });
    }

    public void publishLabelArtifact(ShipmentMessage respShipmentMsg, long startTime, String srcEntityCode) {
        try {
            ShipmentInfo shipmentInfo = respShipmentMsg.getMessageBody().getShipments().get(0);
            ShipmentLabelInfo shipmentLabelInfo = shipmentInfo.getTransitDetails().getLabelDetails();
            String shipmentCorrelationId = shipmentInfo.getShipmentHeader().getShipmentCorrelationId();
            ShipmentArtifact shipmentArtifact = artifactHelper.composeShipmentArtifact(shipmentInfo, shipmentLabelInfo, shipmentCorrelationId, startTime, srcEntityCode);
            log.info("Successfully composed artifact message: {}", shipmentArtifact.toString().replaceAll("(\"contentFromCarrier\":\\s*\")[^\"]*(\")", "'$1******$2'"));
            kafkaArtifactProducer.publishMessage(shipmentArtifact);
        } catch (Exception e) {
            log.error("ShipmentArtifactHelper.publishLabelArtifact failed to publish legacy label shipment artifact message");
        }
    }

    public boolean successShipReqApiCall(ShipmentMessage respShipmentMsg) {
        String requestType = respShipmentMsg.getRequestHeader().getRequestType();
        String responseCode = respShipmentMsg.getRequestHeader().getResponse().getResponseCode();
        return requestType.equals(SHIPMENT_REQ.getValue()) && responseCode.equals(SUCCESS_RESPONSE);
    }

    public boolean validLegacyEntityForLabel(ShipmentMessage respShipmentMsg) {
        List<String> labelEntityCodes = appConfig.getLabelEntityCodes();
        EntityRole entityRole = respShipmentMsg.getMessageHeader().getMessageSources().get(0);
        String entityCode = entityRole.getEntityCode();
        return labelEntityCodes.contains(entityCode.toUpperCase());
    }

}
