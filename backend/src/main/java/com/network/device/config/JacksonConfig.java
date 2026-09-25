package com.network.device.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 对 JSR-310 时间类型的统一格式配置。
 *
 * <p><b>为什么必须有这个类：</b>{@code application.yml} 里的
 * {@code spring.jackson.date-format: yyyy-MM-dd HH:mm:ss} <b>只对
 * {@code java.util.Date} / {@code java.sql.*} 生效，对 {@code LocalDateTime} 无效</b>
 * （Spring Boot 的 {@code Jackson2ObjectMapperBuilder} 内部只是调用
 * {@code ObjectMapper#setDateFormat}，它不改变 {@code JavaTimeModule} 里
 * {@code LocalDateTimeDeserializer} 所用的 {@code ISO_LOCAL_DATE_TIME}）。
 *
 * <p>而前端所有日期时间控件都用 {@code value-format="YYYY-MM-DD HH:mm:ss"}，
 * 提交的是「空格分隔」的字符串。默认反序列化器会直接抛：
 * <pre>
 * JSON parse error: Cannot deserialize value of type `java.time.LocalDateTime`
 * from String "2026-09-22 18:04:47": Text '2026-09-22 18:04:47'
 * could not be parsed at index 10
 * </pre>
 * 于是 {@code POST /api/repairs}（创建工单，携带 {@code faultTime}）会稳定返回 400，
 * 创建工单流程整体不可用。
 *
 * <p>输出侧此前靠各 VO 上的 {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")} 兜住，
 * 但实体（如 {@code NetworkDevice}、{@code DeviceGroup}）作为响应体直接返回时没有该注解，
 * 会输出 ISO 的 {@code 2026-09-22T18:04:47}。这里统一成同一格式，前后端契约收敛为一种。
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsr310LocalDateTimeCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(formatter))
                .deserializers(new LocalDateTimeDeserializer(formatter));
    }
}
