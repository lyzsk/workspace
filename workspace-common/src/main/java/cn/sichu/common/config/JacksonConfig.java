package cn.sichu.common.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
            .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
