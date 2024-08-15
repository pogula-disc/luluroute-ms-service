package com.luluroute.ms.service.dto;

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
public class ShipmentStatusDto {

    private UUID svcDetailId;

    private UUID correlationId;

    private String lstSvcStatus;

    private Date lstSvcStatusDate;

    private String lstSvcStatusBy;
}
