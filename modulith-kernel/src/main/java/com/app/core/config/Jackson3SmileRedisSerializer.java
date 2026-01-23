package com.app.core.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.dataformat.smile.SmileMapper;
import tools.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Custom Redis Serializer using Jackson 3 and SMILE binary format.
 * SMILE reduces payload size significantly compared to JSON.
 */
public class Jackson3SmileRedisSerializer<T> implements RedisSerializer<T> {

    private final SmileMapper mapper;
    private final Class<T> type;

    public Jackson3SmileRedisSerializer(Class<T> type) {
        this.type = type;

        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        // In Jackson 3, use SmileMapper.builder() for SMILE format
        this.mapper = SmileMapper.builder()
                .addModule(new JavaTimeModule())
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            return mapper.writeValueAsBytes(t);
        } catch (Exception e) {
            throw new SerializationException("Could not write SMILE: " + e.getMessage(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return mapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new SerializationException("Could not read SMILE: " + e.getMessage(), e);
        }
    }
}
