package com.luluroute.ms.service.dto;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegacySvcResponse {
    private ShipmentMessage shipmentMessage;
    private boolean success;
    private String message;
}
