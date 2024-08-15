package com.luluroute.ms.service.service.impl;

import com.luluroute.ms.service.dto.ServiceCancelDto;
import com.luluroute.ms.service.dto.ServiceCancelResponse;
import com.luluroute.ms.service.dto.ShipmentSearchDto;
import com.luluroute.ms.service.model.ServiceCancel;
import com.luluroute.ms.service.repository.ServiceCancelRepository;
import com.luluroute.ms.service.service.SvcCancelService;
import com.luluroute.ms.service.util.ObjectMapperUtil;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SvcCancelServiceImpl implements SvcCancelService {

    private final ServiceCancelRepository svcCancelRepository;

    private final String updatedUser = "System User";

    @Override
    public ServiceCancelResponse processServiceCancel(ServiceCancelDto serviceMessage) {
        log.info("Calling processServiceCancel in SvcCancelServiceImpl");
        ServiceCancel svcCancel = ObjectMapperUtil.map(serviceMessage, ServiceCancel.class);
        svcCancelRepository.save(svcCancel);
        log.info("SvcCancel is persisted into DB successfully");
        return ServiceCancelResponse.builder().success(Boolean.TRUE).message("Successfully processed").build();
    }

    @Override
    public void updateServiceCancelResponse(ServiceCancelDto serviceMessage) {
        log.info("Calling updateServiceCancelResponse in SvcCancelServiceImpl");
        serviceMessage.setUpdatedDate(new Date());
        svcCancelRepository.updateCancelResponse(serviceMessage.getResponseCode(),
                serviceMessage.getResponseMessage(), serviceMessage.getResponseDate(),
                serviceMessage.getUpdatedDate(), updatedUser, serviceMessage.getSvcCancelId());
        log.info("SvcCancel response is updated into DB successfully");
    }

    @Override
    public ServiceCancelResponse searchShipmentCancellation(ShipmentSearchDto searchDto) {
        Optional<List<ServiceCancel>> serviceCancels = Optional.empty();
        if (serviceCancels.isPresent()) {
            return ServiceCancelResponse.builder()
                    .serviceDetails(ObjectMapperUtil.mapAll(serviceCancels.get(), ServiceCancelDto.class))
                    .message("ServiceCancel(s) found!").build();
        }
        return ServiceCancelResponse.builder().message("No ServiceCancel found").build();
    }
}
