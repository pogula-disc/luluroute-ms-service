package com.luluroute.ms.service.dto;

import lombok.Data;

@Data
public class KafkaPropsDto {
	private String brokers;
	private String specificAvroReader;
	private String reconnectBackoffms;
	private String reconnectBackoffmaxms;
	private String autoRegisterSchemas;
	private String autoCreateTopics;
	private String securityProtocol;
	private String saslMechanism;
	private String saslJaasConfig3PL;
	private String saslJaasConfigWMS;
	private String schemaRegistryUrl;
	private String schemaUserInfo3PL;
	private String schemaUserInfoWMS;
	private String consumerPollTimeoutms;
}
