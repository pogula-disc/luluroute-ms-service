package com.luluroute.ms.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateShopResponseDto {
	
	@JsonProperty("ResponseMessage")
    public String responseMessage;
	
	@JsonProperty("RateShopType")
    public String rateShopType;

}
