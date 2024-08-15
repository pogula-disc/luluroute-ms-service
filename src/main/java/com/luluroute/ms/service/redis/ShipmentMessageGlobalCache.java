package com.luluroute.ms.service.redis;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import com.luluroute.ms.service.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShipmentMessageGlobalCache {


    final RedisConfig redisConfig;

    final ReactiveRedisOperations<String, ShipmentMessage> reactiveRedisOperations;

    public void save(String shipmentCorrelationId, ShipmentMessage shipmentMessage) {

        String key = redisConfig.keyPrefix + "::" + shipmentCorrelationId;
        ReactiveValueOperations<String, ShipmentMessage> reactiveValueOperations = reactiveRedisOperations.opsForValue();
        reactiveValueOperations.set(key, shipmentMessage, Duration.ofMillis(redisConfig.cacheClearTime)).subscribe();
    }

    public void removeShipmentMessage(String correlationId) {
        log.debug("Remove Redis Key {} ",redisConfig.keyPrefix + "::" + correlationId);
        reactiveRedisOperations.opsForSet().delete(redisConfig.keyPrefix + "::" + correlationId).subscribe();
        reactiveRedisOperations.opsForSet().delete(redisConfig.shipmentResponseKeyPrefix + "::" + correlationId).subscribe();
        reactiveRedisOperations.opsForSet().delete(redisConfig.shipmentCancelResponseKeyPrefix + "::" + correlationId).subscribe();
    }

    public void removeShipmentResponse(String correlationId) {
        reactiveRedisOperations.opsForSet().delete(redisConfig.shipmentResponseKeyPrefix + "::" + correlationId).subscribe();
    }

    public ShipmentMessage getShipmentResponse(String correlationId){
        String key = redisConfig.shipmentResponseKeyPrefix + "::" + correlationId;
        Boolean exists = reactiveRedisOperations.hasKey(key).block();
        if(exists) {
            return reactiveRedisOperations.opsForValue().get(key).block();
        } else {
            return null;
        }
    }

    public ShipmentMessage getShipmentCancelResponse(String correlationId){
        String key = redisConfig.shipmentCancelResponseKeyPrefix + "::" + correlationId;
        Boolean exists = reactiveRedisOperations.hasKey(key).block();
        if(exists) {
            return reactiveRedisOperations.opsForValue().get(key).block();
        } else {
            return null;
        }
    }

    public void removeCancelShipmentResponse(String correlationId) {
        reactiveRedisOperations.opsForSet().delete(redisConfig.shipmentCancelResponseKeyPrefix + "::" + correlationId).subscribe();
    }
}
