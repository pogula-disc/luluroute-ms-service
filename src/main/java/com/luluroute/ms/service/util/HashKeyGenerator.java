package com.luluroute.ms.service.util;

import com.google.common.hash.Hashing;
import com.logistics.luluroute.domain.Shipment.Shared.LocationItem;

import java.nio.charset.StandardCharsets;

public class HashKeyGenerator {

    public static String generateHashKey(LocationItem locationItem) {

        StringBuilder stringBuilder = new StringBuilder(300);
        stringBuilder.append(locationItem.getDescription1());
        stringBuilder.append(locationItem.getDescription2());
        stringBuilder.append(locationItem.getDescription3());
        stringBuilder.append(locationItem.getDescription4());
        stringBuilder.append(locationItem.getDescription5());
        stringBuilder.append(locationItem.getCity());
        stringBuilder.append(locationItem.getState());
        stringBuilder.append(locationItem.getZipCode());
        stringBuilder.append(locationItem.getCountry());
        return Hashing.sha256()
                .hashString(stringBuilder, StandardCharsets.UTF_8).toString();
    }
}
