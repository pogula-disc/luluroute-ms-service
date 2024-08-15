package com.luluroute.ms.service.util;

import java.util.Arrays;

public enum RequestType {
    RATESHOP_REQ("1000"), SHIPMENT_REQ("2000"), SHIP_TO_HOLD("3000"),
    CANCEL_REQ("9989"), PERF_REQ("5500"), RELEASE_SHIPMENT("4000");

    private final String value;

    RequestType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    public static RequestType getRequestTypeByValue(final String val){
        return Arrays.stream(RequestType.values()).filter(value -> value.getValue().equals(val)).findFirst().orElse(null);
    }
}
