package usc.uscPredict.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import usc.uscPredict.filter.JWTFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JWTFilter jwtFilter;

    @Autowired
    public SecurityConfig(JWTFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Authentication endpoints - permitAll
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        // Swagger/OpenAPI/Scalar
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/scalar/**").permitAll()
                        // Actuator health check
                        .requestMatchers("/actuator/health").permitAll()
                        // Public read-only endpoints
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/events/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/markets/**").permitAll()
                        .requestMatchers("/api/v1/orders/**").permitAll() // All methods for testing
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/positions/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/transactions/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/wallets/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/comments/**").permitAll()
                        // Home endpoint
                        .requestMatchers("/api/v1", "/api/v1/").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterAfter(jwtFilter, BasicAuthenticationFilter.class)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
