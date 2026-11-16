package cn.sichu.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "workspace.api-doc")
public class ApiDocProperties {

    private boolean enabled = false;

    private String title = "Workspace API";

    private String version = "1.0.0";

    private String serverUrl = "/";
}
