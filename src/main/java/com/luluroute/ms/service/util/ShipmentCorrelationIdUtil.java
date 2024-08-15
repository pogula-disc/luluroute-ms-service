package com.luluroute.ms.service.util;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.exception.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.Assert;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

import static com.luluroute.ms.service.util.ShipmentConstants.SHIPMENT_STATUS_NEW;
import static com.luluroute.ms.service.util.ShipmentConstants.X_TRANSACTION_REFERENCE;

@Slf4j
public class ShipmentCorrelationIdUtil {

    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    public static void populateShipmentCorrelationId(ShipmentMessage shipmentMessage) {
        // Mandatory header attributes in payload.
        try {
            Assert.notNull(shipmentMessage, "Shipment message cannot be empty!");
            Assert.notNull(shipmentMessage.getMessageHeader(), "Shipment MessageHeader cannot be empty!");
            Assert.notNull(shipmentMessage.getRequestHeader(), "Shipment RequestHeader cannot be empty!");
        } catch (Exception ex) {
            throw new InvalidInputException("Missing mandatory fields");
        }
        log.info("ShipmentCorrelationIdUtil - messageCorrelationId: {}",
                shipmentMessage.getMessageHeader().getMessageCorrelationId());
        shipmentMessage.getMessageBody().getShipments().forEach(shipmentInfo -> {
            if (shipmentInfo.getShipmentStatus().getStatus() == SHIPMENT_STATUS_NEW) {
                String shipmentCorrId = UUID.randomUUID().toString();
                shipmentInfo.getShipmentHeader().setShipmentCorrelationId(shipmentCorrId);
                log.info("ShipmentCorrelationIdUtil - generating new ShipmentCorrelationId: {}", shipmentCorrId);
                MDC.put(X_TRANSACTION_REFERENCE, shipmentCorrId);
            } else {
                MDC.put(X_TRANSACTION_REFERENCE, shipmentInfo.getShipmentHeader().getShipmentCorrelationId());
            }
        });
    }

    public static String uuidToUuid64(String uuidStr) {
        UUID uuid = UUID.fromString(uuidStr);
        byte[] bytes = uuidToBytes(uuid);
        return BASE64_URL_ENCODER.encodeToString(bytes);
    }

    public static String uuid64ToUuid(String uuid64) {
        byte[] decoded = Base64.getUrlDecoder().decode(uuid64);
        UUID uuid = uuidFromBytes(decoded);
        return uuid.toString();
    }

    public static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID uuidFromBytes(byte[] decoded) {
        ByteBuffer bb = ByteBuffer.wrap(decoded);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
