package com.luluroute.ms.service.service;

import java.net.URISyntaxException;

import org.json.JSONException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.luluroute.ms.service.dto.RateShopResponseDto;
import com.luluroute.ms.service.dto.ShipmentS3Input;

public interface SvcRequestRateShopService {

	RateShopResponseDto requestRateShop(String authorizationHeader, ShipmentS3Input messageDto) throws JSONException, JsonMappingException, JsonProcessingException, URISyntaxException;

}
