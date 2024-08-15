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
public class ShipmentCancellationDto {

    private UUID svcDetailId;

    private UUID correlationId;

    private UUID cancellationId;

    private Date cancellationDate;
}
