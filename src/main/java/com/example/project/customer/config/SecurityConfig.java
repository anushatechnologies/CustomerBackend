package com.example.project.customer.config;

import com.example.project.customer.security.FirebaseAccessDeniedHandler;
import com.example.project.customer.security.FirebaseAuthEntryPoint;
import com.example.project.customer.security.FirebaseAuthenticationFilter;
import com.example.project.customer.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final FirebaseAuthEntryPoint firebaseAuthEntryPoint;
    private final FirebaseAccessDeniedHandler firebaseAccessDeniedHandler;

    public SecurityConfig(
            @Autowired(required = false) JwtAuthenticationFilter jwtAuthenticationFilter,
            @Autowired(required = false) FirebaseAuthEntryPoint firebaseAuthEntryPoint,
            @Autowired(required = false) FirebaseAccessDeniedHandler firebaseAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.firebaseAuthEntryPoint = firebaseAuthEntryPoint;
        this.firebaseAccessDeniedHandler = firebaseAccessDeniedHandler;
    }

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
                // Allow preflight CORS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Public: Authentication ──────────────────────────────────────────────
                // Explicitly whitelist only genuinely public endpoints:
                // Pre-login phone check, Firebase sync/exchange, and public logout
                .requestMatchers(HttpMethod.GET, "/api/auth/check-phone").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/sync").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()

                // ── Public: Actuator health (used by deployment health-check) ───────────
                .requestMatchers("/actuator/health").permitAll()

                // ── Public: Product catalogue (read-only GET) ────────────────────────────
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/subcategories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/brands/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/banners/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/promotions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/home/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hot-deals/**").permitAll()

                // ── Public: Location reverse-geocode and serviceability ─
                .requestMatchers("/api/location/**").permitAll()
                .requestMatchers("/api/locations/**").permitAll()
                .requestMatchers("/api/serviceability/**").permitAll()

                // ── Public: Image downloads (product/banner images are public) ──────────
                .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                // ── Admin-only routes (defence-in-depth; controllers also use @PreAuthorize) ─
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // ── Everything else requires authentication ──────────────────────────────
                .anyRequest().authenticated()
        );

        if (jwtAuthenticationFilter != null) {
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnBean(JwtAuthenticationFilter.class)
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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