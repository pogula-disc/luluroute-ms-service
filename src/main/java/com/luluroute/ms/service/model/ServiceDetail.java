package com.luluroute.ms.service.model;

import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "svcdetail", schema = "transit")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDetail {

    @Id
    @Column(name = "svcdetailid", unique = true, nullable = false)
    private UUID svcDetailId;

    @Column(name = "correlationid", nullable = false)
    private String correlationId;

    @Column(name = "svcdetailtype", nullable = false)
    private Integer svcDetailType;

    @Column(name = "reqdate", nullable = false)
    private Date reqDate;

    @Column(name = "reqentitycode", nullable = false)
    private String reqEntityCode;

    @Column(name = "reqaccount")
    private String reqAccount;

    @Column(name = "reqaccountparent")
    private String reqAccountParent;

    @Column(name = "srclocationid", nullable = false)
    private UUID srcLocationId;

    @Column(name = "srcentitycode", nullable = false)
    private String srcEntityCode;

    @Column(name = "srcentitytype")
    private String srcEntityType;

    @Column(name = "dstlocationid", nullable = false)
    private UUID dstLocationId;

    @Column(name = "dstentitycode", nullable = false)
    private String dstEntityCode;

    @Column(name = "dstentitytype")
    private String dstEntityType;

    @Column(name = "shippedate")
    private Date shippedDate;

    @Column(name = "deliverydate")
    private Date deliveryDate;

    @Column(name = "plannedshipdate")
    private Date plannedShipDate;

    @Column(name = "planneddeliverydate")
    private Date plannedDeliveryDate;

    @Column(name = "estimateddeliverydate", nullable = false)
    private Date estimatedDeliveryDate;

    @Column(name = "transitdays", nullable = false)
    private Long transitDays;

    @Column(name = "routerulecode", nullable = false)
    private String routeRuleCode;

    @Column(name = "rateshopcode", nullable = false)
    private String rateShopCode;

    @Column(name = "carriercode", nullable = false)
    private String carrierCode;

    @Column(name = "transitmode", nullable = false)
    private String transitMode;

    @Column(name = "shipmentno")
    private String shipmentNo;

    @Column(name = "shipmentclosedate")
    private Date shipmentCloseDate;

    @Column(name = "trailerno")
    private String trailerNo;

    @Column(name = "trackingno", nullable = false)
    private String trackingNo;

    @Column(name = "masterbol")
    private String masterBol;

    @Column(name = "routeoverridecode")
    private String routeOverrideCode;

    @Column(name = "lstsvcstatus", nullable = false)
    private String lstSvcStatus;

    @Column(name = "lstsvcstatusdate", nullable = false)
    private Date lstSvcStatusDate;

    @Column(name = "lstsvcstatusby", nullable = false)
    private String lstSvcStatusBy;

    @Column(name = "cancellationid")
    private UUID cancellationId;

    @Column(name = "cancellationdate")
    private Date cancellationDate;

    @Column(name = "billid")
    private UUID billId;

    @Column(name = "orderid", nullable = false)
    private String orderId;

    @Column(name = "ordertype", nullable = false)
    private String orderType;

    @Column(name = "orderorigin", nullable = false)
    private String orderOrigin;

    @Column(name = "orderdate", nullable = false)
    private Date orderDate;

    @Column(name = "lpn", nullable = false)
    private String lpn;

    @Column(name = "integration", nullable = false)
    private String integration;

    @Column(name = "ref_1")
    private String ref_1;

    @Column(name = "ref_2")
    private String ref_2;

    @Column(name = "ref_3")
    private String ref_3;

    @Column(name = "active")
    private int active;

    @Column(name = "createddate")
    private Date createdDate;

    @Column(name = "createdby", nullable = false)
    private String createdBy;

    @Column(name = "updateddate")
    private Date updatedDate;

    @Column(name = "updatedby")
    private String updatedBy;

    @PrePersist
    private void prePersist() {
        if(this.createdDate == null)
            this.createdDate = new Date();
        if (this.createdBy == null)
            this.createdBy = "System User";
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedDate = new Date();
    }
}
