package com.luluroute.ms.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ValidatorFactory {
    private static final Map<ShipmentValidatorType, ShipmentValidator> serviceCache = new HashMap<>();

    private final List<ShipmentValidator> handlers;

    @Autowired
    private ValidatorFactory(List<ShipmentValidator> handlers) {
        this.handlers = handlers;
    }

    public static ShipmentValidator getShipmentValidator(ShipmentValidatorType validatorType) {

        ShipmentValidator service = serviceCache.get(validatorType);
        if (null == service) {
            throw new RuntimeException("Unknown validator type: " + validatorType);
        }
        return FactoryUtil.getBean(service.getClass());
    }

    @PostConstruct
    public void initMyServiceCache() {
        handlers.forEach(service -> {
            Validators annotation = service.getClass().getAnnotation(Validators.class);
            ShipmentValidatorType[] commands = annotation.command();
            for (ShipmentValidatorType command : commands) {
                serviceCache.put(command, service);
            }
        });
    }
}