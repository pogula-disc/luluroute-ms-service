package com.luluroute.ms.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix="lulu-route.xml-json")
@Validated
@Data
public class XmlJsonConfig {

    @NotEmpty
    private Map<String, String> carrierIdsByJsonCode;

    private List<String> elementsToRoute2_0;

    private Boolean enableTokenValidation;

    private Boolean enableDefaultDcProfile;

    private String defaultDcProfile;

}
