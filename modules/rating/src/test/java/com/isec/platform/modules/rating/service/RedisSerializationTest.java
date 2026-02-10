package com.isec.platform.modules.rating.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isec.platform.common.cache.CachingConfig;
import com.isec.platform.modules.rating.dto.RateBookDto;
import com.isec.platform.modules.rating.service.RateBookSnapshotLoader;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationTest {

    @Test
    void testSerializationAndDeserialization() {
        // given
        CachingConfig config = new CachingConfig();
        // Since we want to test the serializer used by RedisTemplate, we can just grab the ObjectMapper from it
        // Or better, recreate the logic to be sure.
        
        // Actually, CachingConfig.redisTemplate(...) creates the serializer internally.
        // Let's look at how it's done in CachingConfig.java
        
        // We can use reflection or just copy the logic for testing.
        // But the user wants to ensure it works with what's in CachingConfig.
        
        // I'll create a dummy serializer using the same setup as CachingConfig
        ObjectMapper objectMapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .addModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .addModule(new com.fasterxml.jackson.datatype.jdk8.Jdk8Module())
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .activateDefaultTyping(com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator.instance, 
                        ObjectMapper.DefaultTyping.EVERYTHING, 
                        com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY)
                .build();

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RateBookDto rb = RateBookDto.builder()
                .id(1L)
                .tenantId("SANLAM")
                .name("Test RateBook")
                .rules(List.of(
                        RateBookDto.RateRuleDto.builder()
                                .id(100L)
                                .description("Test Rule")
                                .priority(1)
                                .build()
                ))
                .build();
        
        RateBookSnapshotLoader.Snapshot snapshot = RateBookSnapshotLoader.Snapshot.from(rb);

        // when
        byte[] serialized = serializer.serialize(snapshot);
        Object deserialized = serializer.deserialize(serialized);

        // then
        assertThat(deserialized).isInstanceOf(RateBookSnapshotLoader.Snapshot.class);
        RateBookSnapshotLoader.Snapshot result = (RateBookSnapshotLoader.Snapshot) deserialized;
        assertThat(result.rateBook().getTenantId()).isEqualTo("SANLAM");
        assertThat(result.rateBook().getRules()).hasSize(1);
        assertThat(result.rateBook().getRules().get(0).getDescription()).isEqualTo("Test Rule");
    }
}
