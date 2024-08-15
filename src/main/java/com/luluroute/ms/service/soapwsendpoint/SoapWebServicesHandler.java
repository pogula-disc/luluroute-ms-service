package com.luluroute.ms.service.soapwsendpoint;

import com.luluroute.ms.service.config.XmlJsonConfig;
import com.luluroute.ms.service.exception.ShipmentServiceException;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import com.luluroute.ms.service.service.impl.soapwsproxy.LegacySoapAuthServiceImpl;
import com.luluroute.ms.service.util.AppLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.concurrent.Callable;

import static com.luluroute.ms.service.helper.SoapContextHelper.*;
import static com.luluroute.ms.service.util.ShipmentConstants.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SoapWebServicesHandler {

    private final LegacySoapAuthServiceImpl legacySoapAuthService;

    private final XmlJsonConfig xmlJsonConfig;

    public static long startContext(Object inboundRequest, String requestMessageCorrelationId) {
        long startTime = System.currentTimeMillis();
        MDC.clear(); // Sanity check - in case a thread w/exception left a trace ID
        MDC.put(X_CORRELATION_ID, requestMessageCorrelationId);
        AppLogUtil.getUniqueTraceId(null);
        MDC.put(SOAP_CONTEXT, "true");
        logInboundSoapRequest(inboundRequest);
        return startTime;
    }

    public Object handleSoapRequest(String token, Callable<Node> executeRequest) {
        try {
            log.info("Bypass token validation ! {} token #{}", !xmlJsonConfig.getEnableTokenValidation());
            if(xmlJsonConfig.getEnableTokenValidation() || StringUtils.isNotEmpty(token)) {
                legacySoapAuthService.validateUserFromToken(token);
            }
            return wrapNode(executeRequest.call());
        } catch (LegacySoapWebServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if (e.getExceptionNode() != null) {
                return wrapNode(e.getExceptionNode());
            } else if (e.isInclStackTrace()) {
                return buildErrorResponse(e);
            } else {
                return buildErrorResponse(e.getMessage());
            }
        } catch (ShipmentServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if(e.getCause() == null) {
                return buildErrorResponse(e.getMessage());
            } else {
                return buildErrorResponse(e);
            }
        } catch (Exception e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            return buildErrorResponse(e);
        }
    }

    public Object handleSoapAuthenticationGetTokenRequest(String user, String password) {
        try {
            return legacySoapAuthService.getNewAuthenticationTokenFromLegacy(user, password);
        } catch (LegacySoapWebServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if (e.getExceptionNode() != null) {
                return wrapNode(e.getExceptionNode());
            } else if (e.isInclStackTrace()) {
                return buildErrorResponse(e);
            } else {
                return buildErrorResponse(e.getMessage());
            }
        } catch (ShipmentServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if(e.getCause() == null) {
                return buildErrorResponse(e.getMessage());
            } else {
                return buildErrorResponse(e);
            }
        } catch (Exception e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            return buildErrorResponse(e);
        }
    }

    public Object handleSoapAuthenticationValidateTokenRequest(String token) {
        try {
          return  legacySoapAuthService.validateAuthenticationTokenFromLegacy(token);
        } catch (LegacySoapWebServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if (e.getExceptionNode() != null) {
                return wrapNode(e.getExceptionNode());
            } else if (e.isInclStackTrace()) {
                return buildErrorResponse(e);
            } else {
                return buildErrorResponse(e.getMessage());
            }
        } catch (ShipmentServiceException e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            if(e.getCause() == null) {
                return buildErrorResponse(e.getMessage());
            } else {
                return buildErrorResponse(e);
            }
        } catch (Exception e) {
            log.error(SOAP_UNEXPECTED_ERROR, e);
            return buildErrorResponse(e);
        }
    }

    public static void endContext(Object response, long startTime) {
        logInboundSoapResponse(response);
        log.info(PROCESSING_TIME_SOAP, System.currentTimeMillis() - startTime);
        MDC.clear();
    }
}
