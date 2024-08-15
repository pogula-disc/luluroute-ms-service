package com.luluroute.ms.service.util;

public enum ServiceStatusCode {

    SVC_SUCCESS_200(200, "OK"),
    SVC_SUCCESS_201(201, "Created"),
    SVC_ERROR_500(500, "Server Error"),
    SVC_ERROR_503(503, "Service Unavailable"),
    SVC_ERROR_400(400, "Bad Request"),
    SVC_ERROR_403(403, "Forbidden"),
    SVC_ERROR_404(404, "Not Found"),
    SVC_ERROR_990(990, "Read timeout"),
    SVC_ERROR_999(999, "Other Exception");

    private final int value;
    private final String reasonPhrase;

    ServiceStatusCode(int value, String reasonPhrase) {
        this.value = value;
        this.reasonPhrase = reasonPhrase;
    }

    public int value() {
        return this.value;
    }

    public String getReasonPhrase() {
        return this.reasonPhrase;
    }
}
