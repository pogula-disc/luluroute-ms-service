package com.luluroute.ms.service.dto;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMessageDto {

    private UUID svcMessageId;

    private String svcMessageType;

    private Date svcMessageDate;

    private String ShipmentCorrelationId;

    private ShipmentMessage message;

    private String ref_1;

    private String ref_2;

    private String ref_3;

    private int active;

    private Date createdDate;

    private String createdBy;

    private Date updatedDate;

    private String updatedBy;
}
