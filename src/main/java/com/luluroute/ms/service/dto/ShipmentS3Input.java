package com.luluroute.ms.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.logistics.luluroute.domain.response.entity.EntityProfile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentS3Input {

	@JsonProperty("ShipmentMessage")
	private ShipmentMessage message;
	@JsonProperty("EntityProfile")
	private EntityProfile entityProfile;

}
