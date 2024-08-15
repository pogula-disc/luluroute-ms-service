package com.luluroute.ms.service.model;

import com.logistics.luluroute.avro.shipment.message.ShipmentMessage;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder(toBuilder = true)
@Data
public class MessageResponse implements Serializable {

	private String message;

	private boolean success;

	private ShipmentMessage avroMessage;

	@Override
	public String toString() {
		return "MessageResponse [message=" + message + "]";
	}

}