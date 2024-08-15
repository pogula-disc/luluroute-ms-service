package com.luluroute.ms.service.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "svclocation", schema = "transit")
@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLocation {

    @Id
    @Column(name = "svclocationid", unique = true, nullable = false)
    private UUID svcLocationId;

    @Column(name = "hashkey")
    private String hashKey;

    @Column(name = "locationcode")
    private String locationCode;

    @Column(name = "locationtype", nullable = false)
    private String locationType;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "ownerentitycode")
    private String ownerEntityCode;

    @Column(name = "ownerentitytype")
    private String ownerEntityType;

    @Column(name = "desc1", nullable = false)
    private String desc1;

    @Column(name = "desc2")
    private String desc2;

    @Column(name = "desc3")
    private String desc3;

    @Column(name = "desc4")
    private String desc4;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "zip", nullable = false)
    private String zip;

    @Column(name = "country", nullable = false)
    private String country;

    @Column(name = "ref_1")
    private String ref_1;

    @Column(name = "ref_2")
    private String ref_2;

    @Column(name = "ref_3")
    private String ref_3;

    @Column(name = "active")
    private int active;

    @Column(name = "createddate")
    private LocalDateTime createdDate;

    @Column(name = "createdby", nullable = false)
    private String createdBy;

    @Column(name = "updateddate")
    private LocalDateTime updatedDate;

    @Column(name = "updatedby")
    private String updatedBy;

    @PrePersist
    private void prePersist() {
        if(this.createdDate == null)
            this.createdDate = LocalDateTime.now();
        if (this.createdBy == null)
            this.createdBy = "System User";
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedDate = LocalDateTime.now();
    }
}
