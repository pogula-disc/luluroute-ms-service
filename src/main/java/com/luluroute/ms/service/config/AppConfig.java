package com.luluroute.ms.service.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
public class AppConfig {

    @Value("${lulu-route.shipment-svc.scope.request}")
    private String requestScope;

    @Value("${lulu-route.shipment-svc.scope.labels}")
    private String labelsScope;

    @Value("${lulu-route.shipment-svc.offsetDays}")
    private int offsetDays;

    @Value("${legacy.mock.perfAttrKey}")
    private String perfAttrKey;

    @Value("${legacy.mock.perfAttrValue}")
    private String perfAttrValue;

    @Value("${legacy.shipmentServiceUrl}")
    private String serviceUrl;

    @Value("${lulu-route.shipment-svc.oauth2.accessTokenUri}")
    private String accessTokenUri;

    @Value("#{${lulu-route.shipment-svc.dc.all.entityCodes}}")
    private List<String> dcEntityCodes;

    @Value("#{${lulu-route.shipment-svc.dc.intl.entityCodes}}")
    private List<String> intlDcEntityCodes;

    @Value("#{${lulu-route.shipment-svc.dc.retailOrderTypes}}")
    private List<String> retailOrderTypes;

    @Value("${lulu-route.shipment-svc.entity-codes.message}")
    private List<String> messageEntityCodes;

    @Value("${lulu-route.shipment-svc.entity-codes.origin}")
    private List<String> originEntityCodes;

    @Value("${lulu-route.shipment-svc.entity-codes.origin-SFSCA}")
    private List<String> originEntityCodesSFSCA;

    @Value("${lulu-route.shipment-svc.entity-codes.origin-SFSUS}")
    private List<String> originEntityCodesSFSUS;

    @Value("${lulu-route.shipment-svc.entity-codes.partially-routed}")
    private List<String> partiallyRoutedEntityCodes;

    @Value("${lulu-route.label.entity-codes}")
    private List<String> labelEntityCodes;

    @Value("${shipmentresponse.readTimeoutMillis}")
    private long responseTimeoutMillis;

    @Value("${lulu-route.shipment-svc.military-state-codes}")
    private List<String> militaryStateCodes;

    @Value("${lulu-route.shipment-svc.state-not-mandatory-entityCodes}")
    private List<String> stateNotMandatoryEntityCodes;

    @Value("${lulu-route.international.maxAllowedAmount:2500}")
    private double maxAllowedAmount;

    @Value("${lulu-route.shipment-svc.us-territories}")
    private List<String> usTerritories;

    @Value("${lulu-route.multi-carrier.attributes.enable-for-legacy-json}")
    private boolean enableMultiCarrierAttributesForLegacyJson;

    @Value("${lulu-route.multi-carrier.attributes.enable-for-2_0}")
    private boolean enableMultiCarrierAttributesFor2_0;
}