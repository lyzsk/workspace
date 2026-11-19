package cn.sichu.system.config;

import cn.sichu.system.security.filter.GatewayIntrospectionAuthenticationFilter;
import cn.sichu.system.security.filter.JwtAuthenticationFilter;
import cn.sichu.system.security.filter.TrustedGatewayAuthenticationFilter;
import cn.sichu.system.security.handler.RestAccessDeniedHandler;
import cn.sichu.system.security.handler.RestAuthenticationEntryPoint;
import cn.sichu.system.sync.InternalServiceAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.util.List;

/**
 *
 * @author sichu huang
 * @since 2026/11/14
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GatewayIntrospectionAuthenticationFilter gatewayAuthenticationFilter;
    private final TrustedGatewayAuthenticationFilter trustedGatewayAuthenticationFilter;
    private final InternalServiceAuthenticationFilter internalServiceAuthenticationFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final CorsProperties corsProperties;

    @Value("${workspace.api-doc.enabled:false}")
    private boolean apiDocEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> {
                authorize.requestMatchers("/api/internal/users/sync")
                    .hasAuthority(InternalServiceAuthenticationFilter.AUTHORITY);
                authorize.requestMatchers("/api/internal/authorization/**")
                    .hasAuthority(InternalServiceAuthenticationFilter.AUTHORIZATION_AUTHORITY);
                authorize.requestMatchers("/api/internal/auth/introspect")
                    .hasAuthority(GatewayIntrospectionAuthenticationFilter.AUTHORITY);
                authorize.requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                    "/actuator/health", "/error", "/ws/**").permitAll();
                if (apiDocEnabled) {
                    authorize.requestMatchers("/v3/api-docs/**", "/doc.html", "/webjars/**",
                        "/swagger-ui.html", "/swagger-ui/**", "/swagger-resources/**").permitAll();
                }
                authorize.anyRequest().authenticated();
            })
            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(trustedGatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(internalServiceAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        if (corsProperties.getAllowedOrigins().contains("*")) {
            throw new IllegalStateException("允许携带凭证时 CORS 不能使用通配符 Origin");
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type", "X-Requested-With", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
        PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public Clock authenticationClock() {
        return Clock.systemUTC();
    }
}
