package com.luluroute.ms.service.kafka;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import com.luluroute.ms.service.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.luluroute.ms.service.util.ShipmentConstants.STANDARD_ERROR;

@Component
@Slf4j
public class ShipmentResponseProcessor {

    @Value("${config.shipment.consumer.timeout}")
    private Long consumerTimeout;

    @Value("${config.shipment.response.group.id}")
    private String respConsumerGroupId;

    @Autowired
    private KafkaConfig kafkaConfig;

    /**
     * @param topic
     * @param shipmentCorrelationId
     * @return
     */
    public ShipmentMessage consumeResponseShipmentMessage(String topic, String shipmentCorrelationId) {
        String msg = "ShipmentResponseProcessor.consumeResponseShipmentMessage()";

        Properties properties = kafkaConfig.getConsumerProperties();
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, respConsumerGroupId);
        // One ConsumerGroup - SinglePartition
        try (KafkaConsumer<String, ShipmentMessage> kafkaConsumer = new KafkaConsumer<>(properties)) {
            kafkaConsumer.subscribe(Collections.singletonList(topic));

            long endPollingTimestamp = System.currentTimeMillis() + consumerTimeout;
            log.info(" {} - ShipmentMessage consumer started start : {} - end :{} topic {} for shipmentCorrelationId # {}",
                    msg, System.currentTimeMillis(), endPollingTimestamp, topic, shipmentCorrelationId);

            while (System.currentTimeMillis() < endPollingTimestamp) {
                ConsumerRecords<String, ShipmentMessage> consumerRecords = kafkaConsumer.poll(Duration.ofMillis(1000));
                log.info("Shipment topic # {} | ShipmentMessage messages # {}  ", msg, consumerRecords.count());

                for (ConsumerRecord<String, ShipmentMessage> consumerRecord : consumerRecords) {
                    ShipmentMessage shipmentMessage = consumerRecord.value();
                    if (shipmentMessage == null) {
                        log.warn("Shipment topic # {} | SKIP the bad record", msg);
                    } else {
                        String consumedShipCorrId = shipmentMessage.getMessageBody().getShipments().stream()
                                .map(shipmentInfo -> shipmentInfo.getShipmentHeader().getShipmentCorrelationId())
                                .collect(Collectors.joining());
                        if (shipmentCorrelationId.equalsIgnoreCase(consumedShipCorrId)) {
                            log.info("Found the response! shipmentCorrelationId : {} consumedShipCorrId: {}",
                                    shipmentCorrelationId, consumedShipCorrId);
                            log.info("ShipmentResponse from LuluRoute 2.0: {}", shipmentMessage);
                            return shipmentMessage;
                        } else {
                            log.info("Response doesn't match - shipmentCorrelationId : {} consumedShipCorrId: {}",
                                    shipmentCorrelationId, consumedShipCorrId);

                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(e));
            throw e;
        }
        log.info("End of consumeResponseShipmentMessage: ");

        return null;
    }
}
