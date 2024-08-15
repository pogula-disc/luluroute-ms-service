package com.luluroute.ms.service.config;

import lombok.Data;

import java.util.List;

@Data
public class SoapUserProfile {
    private String userEmail;
    private String userPassword;
    private String dcEntityCode;
    private List<String> dcZipCode;
}
