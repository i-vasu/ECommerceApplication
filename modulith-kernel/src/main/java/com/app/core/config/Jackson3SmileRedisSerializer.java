package com.app.core.config;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.dataformat.smile.SmileMapper;

public class Jackson3SmileRedisSerializer<T> implements RedisSerializer<T> {

    private final SmileMapper mapper;
    private final Class<T> type;

    public Jackson3SmileRedisSerializer(Class<T> type) {
        this.type = type;

        // Secure validator: Limit to your application packages for safety
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.app.")
                .allowIfSubType("java.util.")
                .build();

        // Jackson 3 Builder: JavaTimeModule is registered automatically
        this.mapper = SmileMapper.builder()
                .activateDefaultTyping(ptv, DefaultTyping.NON_FINAL)
                .build();
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) return null;
        try {
            return mapper.writeValueAsBytes(t);
        } catch (Exception e) {
            throw new SerializationException("SMILE Serialization failed: " + e.getMessage(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) return null;
        try {
            if (type == null) {
                return (T) mapper.readValue(bytes, Object.class);
            }
            return mapper.readValue(bytes, type);
        } catch (Exception e) {
            throw new SerializationException("SMILE Deserialization failed: " + e.getMessage(), e);
        }
    }
}