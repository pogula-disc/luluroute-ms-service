package com.luluroute.ms.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentQueryResponse {
    private List<ServiceDetailDto> serviceDetails;
    private String message;
    private boolean success;
}
