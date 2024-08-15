package com.luluroute.ms.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceLocationDto {

    private UUID svcLocationId;

    private String hashKey;

    private String locationCode;

    private String locationType;

    private String description;

    private String ownerEntityCode;

    private String ownerEntityType;

    private String desc1;

    private String desc2;

    private String desc3;

    private String desc4;

    private String city;

    private String state;

    private String zip;

    private String country;

    private String ref_1;

    private String ref_2;

    private String ref_3;

    private int active;

    private LocalDateTime createddate;

    private String createdby;

    private LocalDateTime updatedDate;

    private String updatedBy;
}
