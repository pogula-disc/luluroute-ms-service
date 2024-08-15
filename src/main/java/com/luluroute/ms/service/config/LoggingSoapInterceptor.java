package com.luluroute.ms.service.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

import java.io.ByteArrayOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class LoggingSoapInterceptor implements ClientInterceptor {
    @Override
    public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
        return true;
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {
        ByteArrayOutputStream requestStream = new ByteArrayOutputStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        String stackTrace = ex == null ? "no exception" : ExceptionUtils.getStackTrace(ex);
        try {
            messageContext.getRequest().writeTo(requestStream);
            messageContext.getResponse().writeTo(responseStream);
            if(ex == null) {
                log.info("Outbound SOAP call complete. request: [{}] response: [{}]",
                        requestStream.toString(UTF_8), responseStream.toString(UTF_8));
            } else {
                log.error("Outbound SOAP call error. request: [{}] exception: [{}]",
                        requestStream.toString(UTF_8), stackTrace);
            }
        } catch (Exception e) {
            log.error(String.format(
                    "Unexpected error: %s while attempting to log outbound SOAP req/resp that caused exception: %s",
                    e.getMessage(), stackTrace));
        }
    }
}
