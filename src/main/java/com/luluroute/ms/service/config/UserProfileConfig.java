package com.luluroute.ms.service.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix="config.users")
@Validated
@Data
public class UserProfileConfig {

    @NotNull
    private Set<SoapUserProfile> soapUsers;

    private Map<String, SoapUserProfile> soapUsersById;

    private Map<String, SoapUserProfile> soapUsersByZip;

    private Map<String, SoapUserProfile> soapUsersByDCEntity;

    @PostConstruct
    public void initSoapUserMap() {
        soapUsersById = soapUsers.stream().collect(Collectors.toMap(SoapUserProfile::getUserEmail, Function.identity()));
        if(soapUsersById.isEmpty()) {
            throw new IllegalStateException("""
                    Required config.users.soap-users is missing. Sample config:
                      config:
                        users:
                          soap-users:
                            -
                              user-email: Sumner-luluRoute@lululemon.com
                              dc-entity-code: LAX01
                              dc-zip-code: V4G1A6
                    """);
        }
    }


    @PostConstruct
    public void initSoapUserByDCEntityMap() {
        soapUsersByDCEntity = soapUsers.stream().collect(Collectors.toMap(SoapUserProfile::getDcEntityCode, Function.identity()));
        if(soapUsersByDCEntity.isEmpty()) {
            throw new IllegalStateException("""
                    Required config.users.soap-users is missing. Sample config:
                      config:
                        users:
                          soap-users:
                            -
                              user-email: Sumner-luluRoute@lululemon.com
                              dc-entity-code: LAX01
                              dc-zip-code: V4G1A6
                    """);
        }
    }

    @PostConstruct
    public void initSoapUserByZipMap() {
         for(SoapUserProfile userProfile : soapUsers )
             for (String zipCode : userProfile.getDcZipCode()) {
                 if (null == soapUsersByZip)
                     soapUsersByZip = new HashMap<>();
                 soapUsersByZip.put(zipCode, userProfile);
             }

        if(soapUsersByZip.isEmpty()) {
            throw new IllegalStateException("""
                    Required config.users.soap-users is missing. Sample config:
                      config:
                        users:
                          soap-users:
                            -
                              user-email: Sumner-luluRoute@lululemon.com
                              dc-entity-code: LAX01
                              dc-zip-code: V4G1A6
                    """);
        }
    }
}
