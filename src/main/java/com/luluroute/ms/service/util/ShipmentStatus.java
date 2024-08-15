package com.luluroute.ms.service.util;

public enum ShipmentStatus {
    NOTSET(0),
    NEW(1000),
    UPDATE(3000),
    CANCEL(9989);

    private final long value;

    ShipmentStatus(int value) {
        this.value = value;
    }

    public long value() {
        return this.value;
    }
}
