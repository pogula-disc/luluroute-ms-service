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
public class ServiceCancelDto {
    private UUID svcCancelId;
    private String svcRequestType;
    private Date svcRequestDate;
    private long messageStatus;
    private Date messageStatusDate;
    private String messageCorrelationId;
    private long sequence;
    private long totalSequence;
    private Date messageDate;
    private String roleType;
    private String entityCode;
    private String shipmentCorrelationId;
    private long shipmentStatus;
    private Date shipmentStatusDate;
    private String responseCode;
    private Date responseDate;
    private String responseMessage;
    private Date createdDate;
    private String createdBy;
    private Date updatedDate;
    private String updatedBy;
}
