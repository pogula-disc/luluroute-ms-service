package com.luluroute.ms.service.dto;

import lombok.Data;

@Data
public class KafkaRecord {

	private long offset;
	private int partition;
	private long timestamp;
	private String correlationId;

	public KafkaRecord() {
		// super();
	}

	public KafkaRecord(long offset, int partition, long timestamp, String correlationId) {
		this.offset = offset;
		this.partition = partition;
		this.timestamp = timestamp;
		this.correlationId = correlationId;
	}

}