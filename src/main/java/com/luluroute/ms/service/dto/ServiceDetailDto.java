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
public class ServiceDetailDto {

    private UUID svcDetailId;

    private UUID correlationId;

    private Integer svcDetailType;

    private Date reqDate;

    private String reqEntityCode;

    private String reqAccount;

    private String reqAccountParent;

    private UUID srcLocationId;

    private String srcEntityCode;

    private String srcEntityType;

    private UUID dstLocationId;

    private String dstEntityCode;

    private String dstEntityType;

    private Date shippedDate;

    private Date deliveryDate;

    private Date plannedShipDate;

    private Date plannedDeliveryDate;

    private Date estimatedDeliveryDate;

    private Long transitDays;

    private String routeRuleCode;

    private String routeOverrideCode;

    private String rateShopCode;

    private String carrierCode;

    private String transitMode;

    private String shipmentNo;

    private Date shipmentCloseDate;

    private String trailerNo;

    private String trackingNo;

    private String masterBol;

    private String lstSvcStatus;

    private Date lstSvcStatusDate;

    private String lstSvcStatusBy;

    private UUID cancellationId;

    private Date cancellationDate;

    private UUID billId;

    private String orderId;

    private String orderType;

    private String orderOrigin;

    private Date orderDate;

    private String lpn;

    private String integration;

    private String ref_1;

    private String ref_2;

    private String ref_3;

    private int active;

    private Date createdDate;

    private String createdBy;

    private Date updatedDate;

    private String updatedBy;
}
