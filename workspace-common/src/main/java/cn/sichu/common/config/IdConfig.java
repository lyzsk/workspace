package cn.sichu.common.config;

import cn.sichu.common.id.SnowflakeIdGenerator;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Configuration
public class IdConfig {
    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
        @Value("${workspace.snowflake.worker-id:0}") long workerId,
        @Value("${workspace.snowflake.datacenter-id:0}") long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }

    @Bean
    public IdentifierGenerator identifierGenerator(
        SnowflakeIdGenerator generator) {
        return entity -> generator.nextId();
    }
}
