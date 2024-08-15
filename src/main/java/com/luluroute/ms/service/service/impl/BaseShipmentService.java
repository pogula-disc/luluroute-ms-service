package com.luluroute.ms.service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.Shipment.Shared.EntityRole;
import com.logistics.luluroute.domain.Shipment.Shared.ItemInfo;
import com.luluroute.ms.service.config.AppConfig;
import com.luluroute.ms.service.exception.InvalidInputException;
import com.luluroute.ms.service.util.RequestType;
import com.luluroute.ms.service.validator.ShipmentValidator;
import com.luluroute.ms.service.validator.ShipmentValidatorType;
import com.luluroute.ms.service.validator.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseShipmentService {

    private final AppConfig appConfig;

    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    private static final String resourceName = "ShipResp_TIL01.json";

    public static final int LOWER_BD = 5;

    public static final int UPPER_BD = 15;

    public BaseShipmentService(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    public boolean hasPerformanceAttributes(ShipmentMessage shipmentMessage) {
        boolean isPerfPayload = false;
        try {
            String reqType = shipmentMessage.getRequestHeader().getRequestType();
            List<ItemInfo> itemInfoList = shipmentMessage.getMessageHeader().getExtended();
            if (itemInfoList != null)
                isPerfPayload = itemInfoList.stream().anyMatch(
                        info -> appConfig.getPerfAttrKey().equalsIgnoreCase(info.getKey())
                                && appConfig.getPerfAttrValue().equalsIgnoreCase(info.getValue()))
                        && RequestType.PERF_REQ.toString().equals(reqType);
            if (isPerfPayload) {
                // Added delay time
                int result = random.nextInt(UPPER_BD - LOWER_BD) + LOWER_BD;
                log.info("Performance payload - delay time in ms: {}", result);
                TimeUnit.SECONDS.sleep(result);
            }
        } catch (RuntimeException e) {
            log.error("Exception occurred in hasPerformanceAttributes method {}", e.getMessage());
        } catch (InterruptedException e) {
            log.error("InterruptedException occurred in hasPerformanceAttributes method {}", e.getMessage());
            // Mark the thread when InterruptedException occurs
            Thread.currentThread().interrupt();
        }
        return isPerfPayload;
    }

    public ShipmentValidator getShipmentValidator(ShipmentMessage shipmentMessage) throws InvalidInputException {
        EntityRole entityRole = Optional.ofNullable(shipmentMessage.getMessageHeader().getMessageSources().get(0))
                .orElseThrow(InvalidInputException::new);
        String entityCode = entityRole.getEntityCode();
        String reqType = shipmentMessage.getRequestHeader().getRequestType();

        if(StringUtils.isNotBlank(entityCode) &&
                RequestType.CANCEL_REQ.getValue().equals(reqType))
                return ValidatorFactory.getShipmentValidator(ShipmentValidatorType.SHIPMENT_CANCEL_VALIDATOR);

        // Default validator
        return (StringUtils.isNotBlank(entityCode) && appConfig.getDcEntityCodes().contains(entityCode.toUpperCase())) ?
             ValidatorFactory.getShipmentValidator(ShipmentValidatorType.DC_SHIPMENT_VALIDATOR) :
            ValidatorFactory.getShipmentValidator(ShipmentValidatorType.SFS_SHIPMENT_VALIDATOR);
    }

    public ShipmentMessage getMockedShipmentMessage() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource(resourceName)).getFile());
        return objectMapper.readValue(file, ShipmentMessage.class);
    }

    protected String getJsonString(ShipmentMessage shipmentMessage) throws JsonProcessingException {
        objectMapper.writerWithDefaultPrettyPrinter();
        return objectMapper.writeValueAsString(shipmentMessage);
    }
}
