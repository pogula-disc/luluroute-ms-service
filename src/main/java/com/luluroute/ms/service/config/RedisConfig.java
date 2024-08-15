package com.luluroute.ms.service.config;

import com.logistics.luluroute.domain.Shipment.Message.ShipmentMessage;
import io.lettuce.core.ReadFrom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


/**
 * 
 * @author SathishRaghupathy
 *
 */
@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@EnableCaching
public class RedisConfig {

	@Value("${shipmentmessagecache.redis.host}")
	private String redisHost;

	@Value("${shipmentmessagecache.redis.port}")
	private int redisPort;
	@Value("${shipmentmessagecache.redis.keyPrefix}")
	public String keyPrefix;
	@Value("${shipmentmessagecache.redis.shipmentResponseKeyPrefix}")
	public String shipmentResponseKeyPrefix;

	@Value("${shipmentmessagecache.redis.shipmentCancelResponseKeyPrefix}")
	public String shipmentCancelResponseKeyPrefix;

	@Value("${shipmentmessagecache.redis.ttlInMilliseconds}")
	public long cacheClearTime;

	@Bean
	@ConditionalOnProperty(prefix = "shipmentmessagecache.redis.cluster", name = "enabled", havingValue = "false")
	public LettuceConnectionFactory lettuceConnectionFactory(){
		RedisStandaloneConfiguration redisStandaloneConfiguration =
				new RedisStandaloneConfiguration(redisHost, redisPort);
		return new LettuceConnectionFactory(redisStandaloneConfiguration);
	}

	@Bean
	public ReactiveRedisOperations<String, ShipmentMessage> UrsaFileEntityTemplate(LettuceConnectionFactory lettuceConnectionFactory){
		RedisSerializer<ShipmentMessage> valueSerializer = new Jackson2JsonRedisSerializer<>(ShipmentMessage.class);
		RedisSerializationContext<String, ShipmentMessage> serializationContext = RedisSerializationContext.<String, ShipmentMessage>newSerializationContext(RedisSerializer.string())
				.value(valueSerializer)
				.build();

		return new ReactiveRedisTemplate<>(lettuceConnectionFactory, serializationContext);
	}

	@Bean
	@ConditionalOnProperty(prefix = "shipmentmessagecache.redis.cluster", name = "enabled", havingValue = "true")
	LettuceConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisConfiguration) {
		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
				.readFrom(ReadFrom.REPLICA_PREFERRED).build();
		return new LettuceConnectionFactory(redisConfiguration, clientConfig);
	}

	@Bean
	@ConditionalOnProperty(prefix = "shipmentmessagecache.redis.cluster", name = "enabled", havingValue = "true")
	RedisClusterConfiguration redisConfiguration() {
		List<String> list = new ArrayList<>();
		list.add(redisHost);
		RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration(list);
		redisClusterConfiguration.setMaxRedirects(3);
		return redisClusterConfiguration;
	}

	@Bean
	public RedisTemplate<String, Serializable> redisCacheTemplate(LettuceConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Serializable> template = new RedisTemplate<>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setConnectionFactory(redisConnectionFactory);
		return template;
	}


	@Bean
	public CacheManager cacheManager(RedisConnectionFactory factory) {
		RedisSerializationContext.SerializationPair<Object> jsonSerializer = RedisSerializationContext.SerializationPair
				.fromSerializer(new GenericJackson2JsonRedisSerializer());
		return RedisCacheManager.RedisCacheManagerBuilder.fromConnectionFactory(factory)
				.cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(cacheClearTime))
						.serializeValuesWith(jsonSerializer))
				.build();
	}

}
