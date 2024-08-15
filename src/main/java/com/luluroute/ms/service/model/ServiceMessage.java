package com.luluroute.ms.service.model;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "svcmessage", schema = "transit")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
        @TypeDef(name = "string-array", typeClass = StringArrayType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class ServiceMessage {

    @Id
    @Column(name = "svcmessageid", unique = true, nullable = false)
    private UUID svcMessageId;

    @Column(name = "shipmentcorrelationid", nullable = false)
    private String shipmentCorrelationId;

    @Column(name = "svcmessagetype", nullable = false)
    private Integer svcMessageType;

    @Column(name = "svcmessagedate", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date svcMessageDate;

    @Type(type = "jsonb")
    @Column(name = "message", columnDefinition = "jsonb")
    private ShipmentMessage message;

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

    @Column(name = "shipmentstatus")
    @Nullable
    private String shipmentStatus;

    @Column(name = "originentity")
    @Nullable
    private String originEntity;

    @Column(name = "carriercode")
    @Nullable
    private String carrierCode;

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
