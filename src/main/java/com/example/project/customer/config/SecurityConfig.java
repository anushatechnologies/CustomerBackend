package com.example.project.customer.config;

import com.example.project.customer.security.FirebaseAccessDeniedHandler;
import com.example.project.customer.security.FirebaseAuthEntryPoint;
import com.example.project.customer.security.FirebaseAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final FirebaseAuthenticationFilter firebaseAuthenticationFilter;
    private final FirebaseAuthEntryPoint firebaseAuthEntryPoint;
    private final FirebaseAccessDeniedHandler firebaseAccessDeniedHandler;

    public SecurityConfig(
            @Autowired(required = false) FirebaseAuthenticationFilter firebaseAuthenticationFilter,
            @Autowired(required = false) FirebaseAuthEntryPoint firebaseAuthEntryPoint,
            @Autowired(required = false) FirebaseAccessDeniedHandler firebaseAccessDeniedHandler
    ) {
        this.firebaseAuthenticationFilter = firebaseAuthenticationFilter;
        this.firebaseAuthEntryPoint = firebaseAuthEntryPoint;
        this.firebaseAccessDeniedHandler = firebaseAccessDeniedHandler;
    }

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:5175",
            "https://hinchmart.com",
            "https://www.hinchmart.com"
    );

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (firebaseAuthEntryPoint != null || firebaseAccessDeniedHandler != null) {
            http.exceptionHandling(exceptions -> {
                if (firebaseAuthEntryPoint != null) {
                    exceptions.authenticationEntryPoint(firebaseAuthEntryPoint);
                }
                if (firebaseAccessDeniedHandler != null) {
                    exceptions.accessDeniedHandler(firebaseAccessDeniedHandler);
                }
            });
        }

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().permitAll()
        );

        if (firebaseAuthenticationFilter != null) {
            http.addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}