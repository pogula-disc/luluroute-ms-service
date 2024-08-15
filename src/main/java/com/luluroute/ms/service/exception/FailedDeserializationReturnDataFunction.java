package com.luluroute.ms.service.exception;

import org.springframework.kafka.support.serializer.FailedDeserializationInfo;

import java.util.function.Function;

public class FailedDeserializationReturnDataFunction implements Function<FailedDeserializationInfo, byte[]> {
    public FailedDeserializationReturnDataFunction() {
    }

    public byte[] apply(FailedDeserializationInfo info) {
        return info.getData();
    }
}