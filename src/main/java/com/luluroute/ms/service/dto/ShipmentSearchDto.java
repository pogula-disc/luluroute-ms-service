package com.luluroute.ms.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ShipmentSearchDto {

    private String reqEntityCode;

    private String carrierCode;

    private String transitMode;

    private String trackingNo;

    private String orderId;

    private String lpn;

    private String trailerNo;

    private UUID ShipmentCorrelationId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date serviceDate;
}
