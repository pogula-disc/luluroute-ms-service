package com.luluroute.ms.service.util;

import java.util.Arrays;
import java.util.List;

public enum OrderType {

    ALLOC,
    REPLEN,
    SPECIAL,
    TRUNK,
    ECOMM,
    STRAT;

    public static List<String> getRetailOrderTypeList() {
        return Arrays.asList(ALLOC.name(), REPLEN.name(), SPECIAL.name(), TRUNK.name());
    }

}
