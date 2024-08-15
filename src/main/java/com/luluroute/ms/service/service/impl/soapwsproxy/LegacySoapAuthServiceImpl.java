package com.luluroute.ms.service.service.impl.soapwsproxy;

import com.enroutecorp.ws.outbound.AuthenticationGetTokenResponse;
import com.enroutecorp.ws.outbound.AuthenticationValidateTokenResponse;
import com.luluroute.ms.service.exception.soapwsexception.LegacySoapWebServiceException;
import com.luluroute.ms.service.soapwsclient.LegacySoapClient;
import com.luluroute.ms.service.util.RedisCacheLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.time.Instant;
import java.time.ZonedDateTime;

import static com.luluroute.ms.service.util.LegacyXmlDocumentConstants.*;
import static com.luluroute.ms.service.util.ShipmentConstants.*;
import static java.time.format.DateTimeFormatter.ofPattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class LegacySoapAuthServiceImpl {

    private final LegacySoapClient legacySoapClient;
    private final RedisCacheLoader redisCacheLoader;

    public void validateUserFromToken(String token) throws LegacySoapWebServiceException {
        log.debug("Authenticating token {}", token);
        AuthDetails authDetails = redisCacheLoader.getAuthenticationDetails(token);
        if (authDetails == null || isExpired(authDetails)) {
            // Even when the token is expired, pass it to Legacy in order to populate a full failure response
            authDetails = getAuthenticationDetailsFromLegacy(token);
        }
        setUser(authDetails);
    }

    public AuthDetails getTokenForUser(String userId,String password) throws LegacySoapWebServiceException {
        log.info("Authenticating userId {}", userId);
        AuthDetails authDetails = redisCacheLoader.getAuthenticationDetailsForUserId(userId);
        if (authDetails == null || isExpired(authDetails)) {
            log.info("Calling legacy API for new token. userId {}", userId);
            // Even when the token is expired, pass it to Legacy in order to populate a full failure response
            authDetails = getAuthenticationDetailsForUserIdFromLegacy(userId, password);
        }
        setUser(authDetails);
        return authDetails;
    }

    private AuthDetails getAuthenticationDetailsFromLegacy(String token) throws LegacySoapWebServiceException {
        log.info("Retrieving token auth from Legacy {}", token);
        Node authenticationNode = (Node) legacySoapClient
                .callAuthenticationValidateToken(token)
                .getAuthenticationValidateTokenResult()
                .getContent()
                .get(0);
        Node validTokenNode = getChildElement(authenticationNode, ELEMENT_VALID_TOKEN);
        checkForFailure(authenticationNode, validTokenNode);
        AuthDetails authDetails = new AuthDetails(token, getUserFromTokenNode(validTokenNode), getExpiryFromTokenNode(validTokenNode));
        cacheNewToken(authDetails);
        return authDetails;
    }

    private AuthDetails getAuthenticationDetailsForUserIdFromLegacy(String userId, String password) throws LegacySoapWebServiceException {
        log.info("Retrieving token from Legacy {}", userId);
        Node authenticationNode = (Node) legacySoapClient
                .callNewAuthenticationGetToken(userId, password)
                .getAuthenticationGetTokenResult()
                .getContent()
                .get(0);
        Node validTokenNode = getChildElement(authenticationNode, ELEMENT_VALID_TOKEN);
        checkForFailure(authenticationNode, validTokenNode);
        AuthDetails authDetails = new AuthDetails(getTokenFromTokenNode(validTokenNode), getUserFromTokenNode(validTokenNode), getExpiryFromTokenNode(validTokenNode));
        cacheNewToken(authDetails);
        return authDetails;
    }


    public AuthenticationValidateTokenResponse validateAuthenticationTokenFromLegacy(String token) throws LegacySoapWebServiceException {
        log.info("Validating auth token from Legacy {}", token);
        return legacySoapClient
                .callAuthenticationValidateToken(token);
    }

    public AuthenticationGetTokenResponse getNewAuthenticationTokenFromLegacy(String user, String password) throws LegacySoapWebServiceException {
        log.info("New token auth from Legacy {}", user);
        return legacySoapClient
                .callNewAuthenticationGetToken(user, password);
    }

    private void checkForFailure(Node authenticationNode, Node validTokenNode) throws LegacySoapWebServiceException {
        Node statusNode = getChildElement(authenticationNode, ELEMENT_STATUS);
        if (statusNode == null
                || validTokenNode == null
                || Integer.parseInt(statusNode.getTextContent()) == 0
                || StringUtils.isEmpty(getUserFromTokenNode(validTokenNode))) {
            throw new LegacySoapWebServiceException(LEGACY_AUTHENTICATION_FAILED, null, authenticationNode, false);
        }
    }

    private void cacheNewToken(AuthDetails authDetails) {
        try {
            redisCacheLoader.addAuthenticationDetails(authDetails);
            redisCacheLoader.addAuthenticationDetailsForUserId(authDetails);
        } catch (Exception e) {
            log.error("Unable to cache token {}", authDetails.token, e);
        }
    }

    private boolean isExpired(AuthDetails authDetails) {
        if(Instant.ofEpochSecond(authDetails.expiry).isBefore(Instant.now())) {
            log.error("Token expired {}", authDetails.token);
            redisCacheLoader.clearAuthenticationDetails(authDetails.token);
            return true;
        }
        return false;
    }

    private void setUser(AuthDetails authDetails) {
        MDC.put(USER_ID, authDetails.userId);
        log.debug(LEGACY_AUTHENTICATION_SUCCESS);
    }

    private Node getChildElement(Node node, String elementName) {
        if (node == null || node.getOwnerDocument() == null) {
            return null;
        }
        NodeList nodeList = node.getOwnerDocument().getElementsByTagName(elementName);
        return nodeList == null ? null : nodeList.item(0);
    }

    private String getUserFromTokenNode(Node validTokenNode) {
        return validTokenNode.getAttributes().getNamedItem(ATTRIBUTE_USER).getTextContent();
    }

    private String getTokenFromTokenNode(Node validTokenNode) {
        return validTokenNode.getAttributes().getNamedItem(ATTRIBUTE_TOKEN).getTextContent();
    }

    private long getExpiryFromTokenNode(Node validTokenNode) {
        String expiryGMT = validTokenNode.getAttributes().getNamedItem(ATTRIBUTE_EXPIRY).getTextContent();
        return ZonedDateTime.parse(expiryGMT, ofPattern("yyyy-MM-dd HH:mm:ssX")).toEpochSecond();
    }

    public record AuthDetails (String token, String userId, long expiry) { }

}
