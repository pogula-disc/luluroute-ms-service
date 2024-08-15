package com.luluroute.ms.service.kafka;

import com.logistics.luluroute.avro.artifact.message.ShipmentArtifact;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 
 * @author CLEE14
 *
 */
@Slf4j
@Component
public class KafkaArtifactProducer {

	@Autowired
	@Qualifier("artifactKafkaTemplate")
	private KafkaTemplate<String, ShipmentArtifact> kafkaTemplate;

	@Value("${config.producer.topic}")
	private String topicName;

	public void publishMessage(ShipmentArtifact artifactData) {
		log.debug("Trying to publish message to kafka topic {}", topicName);
		String shipmentCorrelationId = artifactData.getArtifactHeader().getShipmentCorrelationId().toString();
		try {
			kafkaTemplate.send(topicName, shipmentCorrelationId,
					artifactData);
			log.info("Artifact message published for shipmentCorrelationId: {}", shipmentCorrelationId);
		} catch (Exception e) {
			log.error("Error publishing message to topic {} for shipmentCorrelationId: {} ... failure message  {}", topicName, shipmentCorrelationId,
					ExceptionUtils.getStackTrace(e));
		}
		log.debug("Successfully published message.");
	}
}