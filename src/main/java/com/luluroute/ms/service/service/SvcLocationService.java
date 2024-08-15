package com.luluroute.ms.service.service;

import com.luluroute.ms.service.dto.ServiceLocationDto;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;

import java.util.UUID;

public interface SvcLocationService {

    UUID getSvcLocationIdByHashKey(String hashKey);

    ShipmentServiceResponse createSvcLocation(ServiceLocationDto locationDto);
}
