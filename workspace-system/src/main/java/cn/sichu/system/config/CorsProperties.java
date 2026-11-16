package cn.sichu.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.cors")
public class CorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
}
