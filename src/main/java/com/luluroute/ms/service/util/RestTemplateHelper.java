package com.luluroute.ms.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.UnknownHttpStatusCodeException;

import static com.luluroute.ms.service.util.ShipmentConstants.STANDARD_ERROR;
import static com.luluroute.ms.service.util.ShipmentConstants.STANDARD_INFO;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestTemplateHelper<V> {

    private final RestTemplate restTemplate;

    @Retryable(include = {HttpServerErrorException.class,
            UnknownHttpStatusCodeException.class},
            maxAttemptsExpression = "${restTemplate.maxAttempts:3}",
            recover = "handlePerformRestCallException")
    public V performGetCall(String url, String params, HttpHeaders headers, Class<V> responseType) {
        String msg = "RestTemplateHelper.performGetCall()";
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<V> response =
                restTemplate.exchange(url + params, HttpMethod.GET, requestEntity, responseType);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error(String.format(STANDARD_INFO, msg, url, response));
        }
        return response.getBody();
    }

    @Recover
    public Object handlePerformRestCallException(Exception exception, String url, String params) throws Exception {
        String msg = "RestTemplateHelper.performGetCall()";
        log.error(STANDARD_ERROR, msg, ExceptionUtils.getStackTrace(exception));
        throw exception;
    }

}
