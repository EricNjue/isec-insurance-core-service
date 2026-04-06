package com.isec.platform.common.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CachingConfig {

    public static final String SANLAM_DOUBLE_INSURANCE_CACHE = "sanlamDoubleInsurance";
    public static final String MASTER_REFERENCE_DATA_CACHE = "masterReferenceData";
    public static final String DEPENDENT_REFERENCE_DATA_CACHE = "dependentReferenceData";

    @Value("${integrations.sanlam.double-insurance-cache-expiry-minutes:15}")
    private long sanlamDoubleInsuranceExpiry;

    @Value("${integrations.reference-data.master-cache-expiry-hours:24}")
    private long masterReferenceDataExpiry;

    @Value("${integrations.reference-data.dependent-cache-expiry-hours:24}")
    private long dependentReferenceDataExpiry;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(createCacheObjectMapper());

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(createCacheObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(SANLAM_DOUBLE_INSURANCE_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(sanlamDoubleInsuranceExpiry)));
        cacheConfigurations.put(MASTER_REFERENCE_DATA_CACHE, defaultConfig.entryTtl(Duration.ofHours(masterReferenceDataExpiry)));
        cacheConfigurations.put(DEPENDENT_REFERENCE_DATA_CACHE, defaultConfig.entryTtl(Duration.ofHours(dependentReferenceDataExpiry)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Creates a specialized ObjectMapper for Redis caching that includes type information.
     * This is separate from the primary web ObjectMapper to avoid leaking type info in API responses.
     */
    private ObjectMapper createCacheObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Use EVERYTHING to include type info for all types including DTOs and Records.
        // This ensures '@class' is always present for deserialization from Redis into Object.
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
        return objectMapper;
    }

    /**
     * Primary ObjectMapper used by Spring MVC for API responses.
     * Does NOT include type information, ensuring clean JSON for clients.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Standard JSON output without @class or type wrappers
        return objectMapper;
    }
}
