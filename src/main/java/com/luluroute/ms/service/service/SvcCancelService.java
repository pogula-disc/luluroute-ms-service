package com.luluroute.ms.service.service;

import com.luluroute.ms.service.dto.ServiceCancelDto;
import com.luluroute.ms.service.dto.ServiceCancelResponse;
import com.luluroute.ms.service.dto.ShipmentSearchDto;

public interface SvcCancelService {
    ServiceCancelResponse processServiceCancel(ServiceCancelDto serviceMessage);

    void updateServiceCancelResponse(ServiceCancelDto serviceMessage);

    ServiceCancelResponse searchShipmentCancellation(ShipmentSearchDto searchDto);
}
