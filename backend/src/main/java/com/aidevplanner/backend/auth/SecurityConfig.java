package com.aidevplanner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Instant;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Value("${app.security.enabled:true}") boolean securityEnabled
    ) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health", "/api/auth/**", "/v3/api-docs/**", "/swagger-ui/**")
                .permitAll()
                .anyRequest()
                .authenticated()
        )
        .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    objectMapper.writeValue(response.getWriter(), Map.of(
                            "status", "UNAUTHORIZED",
                            "message", "Authentication is required.",
                            "errors", Map.of("auth", "Missing or invalid bearer token."),
                            "timestamp", Instant.now().toString()
                    ));
                })
        )
        jwtAuthenticationFilter.ifAvailable(filter ->
                http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
        );
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
