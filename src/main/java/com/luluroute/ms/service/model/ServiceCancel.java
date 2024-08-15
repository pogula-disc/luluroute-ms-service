package com.luluroute.ms.service.model;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "svccancel", schema = "transit")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@TypeDefs({
        @TypeDef(name = "string-array", typeClass = StringArrayType.class),
        @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
})
public class ServiceCancel {

    @Id
    @Column(name = "svc_cancel_id", unique = true, nullable = false)
    private UUID svcCancelId;

    @Column(name = "svc_request_type", nullable = false)
    private String svcRequestType;

    @Column(name = "svc_request_date", nullable = false)
    private Date svcRequestDate;

    @Column(name = "message_status")
    private long messageStatus;

    @Column(name = "message_status_date")
    private Date messageStatusDate;
    @Column(name = "message_correlation_id")
    private String messageCorrelationId;

    @Column(name = "sequence")
    private long sequence;

    @Column(name = "total_sequence")
    private long totalSequence;

    @Column(name = "message_date")
    private Date messageDate;

    @Column(name = "role_type")
    private String roleType;

    @Column(name = "entity_code")
    private String entityCode;

    @Column(name = "shipment_correlation_id", nullable = false)
    private String shipmentCorrelationId;

    @Column(name = "shipment_status")
    private long shipmentStatus;

    @Column(name = "shipment_status_date")
    private Date shipmentStatusDate;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "response_date")
    private Date responseDate;

    @Column(name = "response_message")
    private String responseMessage;
    @Column(name = "created_date")
    private Date createdDate;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_date")
    private Date updatedDate;

    @Column(name = "updated_by")
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
