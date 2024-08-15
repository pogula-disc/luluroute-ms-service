package com.luluroute.ms.service.util;

import com.logistics.luluroute.redis.shipment.entity.EntityPayload;
import com.luluroute.ms.service.service.impl.soapwsproxy.LegacySoapAuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import static com.luluroute.ms.service.util.ShipmentConstants.MESSAGE_REDIS_KEY_LOADING;


@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheLoader {

	@Cacheable(cacheNames = "MSE01-PROFILE", key = "#key", unless = "#result == null")
	public EntityPayload getEntityByCode(String key) {
		log.info(String.format(MESSAGE_REDIS_KEY_LOADING, "Entity Profile", key));
		return null;
	}

	@Cacheable(cacheNames = "MSS01-SOAP-TOKEN", key = "#token", unless = "#result == null")
	public LegacySoapAuthServiceImpl.AuthDetails getAuthenticationDetails(String token) {
		return null;
	}

	@Cacheable(cacheNames = "MSS01-SOAP-USER", key = "#userId", unless = "#result == null")
	public LegacySoapAuthServiceImpl.AuthDetails getAuthenticationDetailsForUserId(String userId) {
		return null;
	}

	@CachePut(cacheNames = "MSS01-SOAP-TOKEN", key = "#authDetails.token")
	public LegacySoapAuthServiceImpl.AuthDetails addAuthenticationDetails(LegacySoapAuthServiceImpl.AuthDetails authDetails) {
		log.debug("Adding to cache SOAP token {}", authDetails.token());
		return authDetails;
	}

	@CachePut(cacheNames = "MSS01-SOAP-USER", key = "#authDetails.userId")
	public LegacySoapAuthServiceImpl.AuthDetails addAuthenticationDetailsForUserId(LegacySoapAuthServiceImpl.AuthDetails authDetails) {
		log.debug("Adding to cache SOAP token {} {}", authDetails.token(), authDetails.userId());
		return authDetails;
	}

	@CacheEvict(cacheNames = "MSS01-SOAP-TOKEN", key = "#token")
	public void clearAuthenticationDetails(String token) {
		log.debug("Clearing cache for invalid SOAP token {}", token);
	}

}
