package cn.sichu.system.app.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 *
 * @author sichu huang
 * @since 2026/11/16
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "workspace.eap-tool-service")
public class EapToolServiceTicketProperties {

    @NotBlank
    private String uiUrl = "/eap/";

    @NotBlank
    private String ticketSecret = "dev-only-eap-tool-service-ticket-secret-0123456789abcdef";

    @NotBlank
    private String issuer = "workspace";

    @NotBlank
    private String audience = "eap-tool-service";

    private Duration ticketTtl = Duration.ofSeconds(60);
}
