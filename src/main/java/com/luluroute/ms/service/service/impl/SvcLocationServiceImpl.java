package com.luluroute.ms.service.service.impl;

import com.luluroute.ms.service.dto.ServiceLocationDto;
import com.luluroute.ms.service.dto.ShipmentServiceResponse;
import com.luluroute.ms.service.repository.ServiceLocationRepository;
import com.luluroute.ms.service.service.SvcLocationService;
import com.luluroute.ms.service.service.processor.ShipmentMessageBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SvcLocationServiceImpl implements SvcLocationService {

    private final ShipmentMessageBuilder messageProcessor;

    private final ServiceLocationRepository locationRepository;

    @Autowired
    public SvcLocationServiceImpl(ShipmentMessageBuilder processor, ServiceLocationRepository repository) {
        this.messageProcessor = processor;
        this.locationRepository = repository;
    }

    @Override
    public UUID getSvcLocationIdByHashKey(String hashKey) {
        Optional<UUID> optLocationId = locationRepository.findServiceLocationIdByHashKey(hashKey);
        return optLocationId.orElse(null);
    }

    @Override
    public ShipmentServiceResponse createSvcLocation(ServiceLocationDto locationDto) {
        Optional<UUID> dtoOptional = locationRepository
                .findServiceLocationIdByHashKey(locationDto.getHashKey());
        if (dtoOptional.isPresent()) {

        }
        return null;
    }
}
