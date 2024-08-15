package com.luluroute.ms.service.kafka;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static com.luluroute.ms.service.util.ShipmentConstants.*;

@Slf4j
@Service
public class KafkaShipmentProducer {

    @Autowired
    @Qualifier("kafkaTemplate")
    private KafkaTemplate<String, ShipmentMessage> kafkaTemplate;

    @Value("${config.shipment.source.topic}")
    private String sourceTopic;

    public void publishMessage(ShipmentMessage avroShipment, String shipmentCorrelationId,
                               String msgEntityCode, String originCode, String carrierCode) {
        log.debug("APP_MESSAGE=\"Entering KafkaShipmentProducer publishMessage with shipmentCorrelationId: {}\"",
                shipmentCorrelationId);
        try {
            ListenableFuture<SendResult<String, ShipmentMessage>> respFuture;
            RecordHeaders headers = new RecordHeaders();
            String createdTime = Instant.now().toString();
            String reqType = avroShipment.getRequestHeader().getRequestType().toString();
            String partitionKey = new StringBuilder(msgEntityCode).append(KEY_DELIMITER).append(originCode)
                    .append(KEY_DELIMITER).append(shipmentCorrelationId).toString();
            headers.add(KafkaHeaders.CORRELATION_ID, shipmentCorrelationId.getBytes(StandardCharsets.UTF_8));
            headers.add(REQUEST_TYPE, reqType.getBytes(StandardCharsets.UTF_8));
            headers.add(TS_CREATED, createdTime.getBytes(StandardCharsets.UTF_8));
            headers.add(MSG_ENTITY_CODE, msgEntityCode.getBytes(StandardCharsets.UTF_8));
            headers.add(ORIGIN_ENTITY_CODE, originCode.getBytes(StandardCharsets.UTF_8));
            headers.add(CARRIER_CODE, (null != carrierCode) ? carrierCode.getBytes(StandardCharsets.UTF_8) : "".getBytes());

            ProducerRecord<String, ShipmentMessage> record = new ProducerRecord<>(sourceTopic, null, partitionKey,
                    avroShipment, headers);

            log.info("Publishing with partitionKey: {}", partitionKey);

            kafkaTemplate.send(record);

            log.info("Successfully published Shipment message to {} topic", sourceTopic);
        } catch (Exception e) {
            log.error("Error publishing message to topic {} for shipmentCorrelationId: {} ... failure message  {}", sourceTopic, shipmentCorrelationId,
                    ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

}
