package com.luluroute.ms.service.dto;

import lombok.Data;

import java.util.List;

@Data
public class ContractDto {

	private String topicName;
	private String retryName;
	private String startTime;
	private String endTime;
	private String consumerName;
	private String projectCode;
	private String partition;
	private String offset;
	private List<KafkaRecord> kafkaRecords;
	private List<Integer> selectedOffsets;
	private String timespan;
	private String userId;
	private String correlationId;
}
