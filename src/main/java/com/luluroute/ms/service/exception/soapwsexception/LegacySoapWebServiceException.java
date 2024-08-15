package com.luluroute.ms.service.exception.soapwsexception;

import lombok.Getter;
import org.w3c.dom.Node;

public class LegacySoapWebServiceException extends Exception {

    @Getter
    private final Node exceptionNode;
    @Getter
    private final boolean inclStackTrace;

    public LegacySoapWebServiceException(String message, Throwable cause, Node exceptionNode, boolean inclStackTrace) {
        super(message, cause);
        this.exceptionNode = exceptionNode;
        this.inclStackTrace = inclStackTrace;
    }
}
