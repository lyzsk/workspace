package cn.sichu.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/19
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.cors")
public class AuthCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:5173"));
}
