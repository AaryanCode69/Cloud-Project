package com.example.product_service.config;

import com.example.product_service.exception.ApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] READ_AUTHORITIES = {
            "SCOPE_CloudProject.Access",
            "SCOPE_CloudProject.Write",
            "APPROLE_CloudProject.Admin"
    };

    private static final String[] WRITE_AUTHORITIES = {
            "SCOPE_CloudProject.Write",
            "APPROLE_CloudProject.Admin"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").hasAnyAuthority(READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyAuthority(WRITE_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyAuthority(WRITE_AUTHORITIES)
                        .requestMatchers("/api/products/**").denyAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeSecurityError(
                                response,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                "Unauthorized",
                                "A valid Microsoft Entra ID bearer token is required.",
                                request.getRequestURI(),
                                true
                        ))
                        .accessDeniedHandler((request, response, exception) -> writeSecurityError(
                                response,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                "Forbidden",
                                "The authenticated token does not include the required scope or app role.",
                                request.getRequestURI(),
                                false
                        )))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<GrantedAuthority> authorities = new ArrayList<>(scopeConverter.convert(jwt));
            Optional.ofNullable(jwt.getClaimAsStringList("roles"))
                    .orElseGet(List::of)
                    .stream()
                    .map(role -> new SimpleGrantedAuthority("APPROLE_" + role))
                    .forEach(authorities::add);
            return authorities;
        });
        return authenticationConverter;
    }

    private void writeSecurityError(
            HttpServletResponse response,
            ObjectMapper objectMapper,
            HttpStatus status,
            String error,
            String message,
            String path,
            boolean bearerChallenge
    ) throws IOException {
        if (bearerChallenge) {
            response.setHeader("WWW-Authenticate", "Bearer");
        }
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                path
        ));
    }
}
