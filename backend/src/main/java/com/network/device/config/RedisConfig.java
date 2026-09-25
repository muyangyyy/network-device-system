package com.network.device.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 这里原先调用了：
        //   objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
        //                                      ObjectMapper.DefaultTyping.NON_FINAL);
        // 该配置会在序列化结果里写入 Java 全限定类名（@class 字段），反序列化时据此实例化任意类型，
        // 而 LaissezFaireSubTypeValidator 对类名不做任何白名单限制——这是 Jackson 官方明确警示的
        // 反序列化利用面：只要能向 Redis 写入数据，就可能触发任意类加载。
        // 本项目目前没有任何业务代码使用 RedisTemplate（缓存/会话/限流均未落地），
        // 因此这个风险属于「零收益、有代价」，直接移除。将来若确实需要多态缓存，
        // 应改用 activateDefaultTyping 的白名单重载，或对每个值类型显式指定 @JsonTypeInfo。
        objectMapper.registerModule(new JavaTimeModule());

        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }
}
