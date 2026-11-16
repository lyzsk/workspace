package cn.sichu.system.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;

/**
 *
 * @author sichu huang
 * @since 2026/11/15
 */
@Configuration
@ConditionalOnProperty(prefix = "workspace.api-doc", name = "enabled", havingValue = "true")
public class ApiDocConfig {

    @Bean
    public OpenAPI workspaceOpenAPI(ApiDocProperties properties) {
        SecurityScheme authorization = new SecurityScheme()
            .name(HttpHeaders.AUTHORIZATION)
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .scheme("Bearer")
            .bearerFormat("JWT");
        return new OpenAPI()
            .info(new Info().title(properties.getTitle()).version(properties.getVersion()))
            .addServersItem(new Server().url(properties.getServerUrl()))
            .components(new Components().addSecuritySchemes(HttpHeaders.AUTHORIZATION, authorization))
            .addSecurityItem(new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION));
    }

    @Bean
    public GlobalOpenApiCustomizer workspaceSecurityCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations()
                    .forEach(operation -> operation.addSecurityItem(
                        new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION))));
            }
        };
    }

    @Bean
    public GroupedOpenApi systemOpenApi() {
        return GroupedOpenApi.builder()
            .group("system")
            .displayName("系统管理")
            .packagesToScan("cn.sichu.system")
            .build();
    }
}
