package com.luluroute.ms.service.service;

import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.service.util.RedisCacheLoader;
import com.luluroute.ms.service.util.RestTemplateHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.logistics.luluroute.util.ConstantUtil.STANDARD_FIELD_INFO;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRehydrateService {

    @Value("${config.serviceurl.entity}")
    private String entityUrl;
    private final RestTemplateHelper<EntityPayload> entityPayloadRestPublisher;
    private final RedisCacheLoader redisCacheLoader;

    /**
     * Gets the entity by code.
     *
     * @param key the entity code
     * @return the entity by code
     */
    public EntityPayload getEntityByCode(String key) {
        EntityPayload entityPayload = redisCacheLoader.getEntityByCode(key);
        log.debug(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from Redis", key));
        if (ObjectUtils.isEmpty(entityPayload)) {
            log.debug(String.format(STANDARD_FIELD_INFO, "Loading Store Profile from DB", key));
            HttpHeaders headers = prepareHeaders();
            entityPayload = entityPayloadRestPublisher.performGetCall(entityUrl, key, headers, EntityPayload.class);
        }
        return entityPayload;
    }

    public HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
