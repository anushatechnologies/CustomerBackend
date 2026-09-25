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
                // Allow preflight CORS requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Public: Authentication ──────────────────────────────────────────────
                // /api/auth/** is permitAll here; AuthController handles its own 401 logic
                // for endpoints that truly require a token (e.g. /me).
                .requestMatchers("/api/auth/**").permitAll()

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
                .requestMatchers(HttpMethod.GET, "/api/specifications/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/coupons/**").permitAll()

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