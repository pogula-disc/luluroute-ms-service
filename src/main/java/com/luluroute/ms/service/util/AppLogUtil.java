package com.luluroute.ms.service.util;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.util.Objects;
import java.util.UUID;

@Slf4j
public class AppLogUtil {

    public static String getUniqueTraceId(ShipmentMessage shipmentMessage) {
        String traceId;

        if (Objects.nonNull(shipmentMessage)
                && Objects.nonNull(shipmentMessage.getMessageHeader())
                && StringUtils.isNotBlank(shipmentMessage.getMessageHeader().getMessageCorrelationId())) {
            traceId = shipmentMessage.getMessageHeader().getMessageCorrelationId();
            MDC.put(ShipmentConstants.X_CORRELATION_ID, traceId);
        } else {
            traceId = getCurrentTraceId();
        }

        return traceId;
    }

    public static String getCurrentTraceId() {
        String id = MDC.get(ShipmentConstants.X_CORRELATION_ID);
        if (StringUtils.isBlank(id)) {
            id = UUID.randomUUID().toString();
            log.info("Generating new unique traceId: {}", id);
            MDC.put(ShipmentConstants.X_CORRELATION_ID, id);
        }
        return id;
    }
}
