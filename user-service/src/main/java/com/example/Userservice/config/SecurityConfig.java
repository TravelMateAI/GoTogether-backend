package com.example.Userservice.config; // Renamed

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Import
import org.springframework.security.crypto.password.PasswordEncoder;   // Import

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = true) // Added prePostEnabled for @PreAuthorize
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    // Permit actuator endpoints
                    .requestMatchers("/actuator/**").permitAll()
                    // Secure gRPC endpoints if using net.devh.boot.grpc.server.security.check.AccessPredicate
                    // For now, assume gRPC security is handled by method annotations or a separate mechanism
                    // if not covered by Spring Security's HTTP filter chain directly for gRPC.
                    // If gRPC goes through a Spring MVC dispatcher (e.g. Armeria), rules here might apply.
                    // With net.devh.boot.grpc.server.autoconfigure.GrpcServerSecurityAutoConfiguration,
                    // Spring method security (@PreAuthorize) should work on @GrpcService methods.
                    .requestMatchers("/grpc/**").permitAll() // Keep this if method security is the primary guard for gRPC
                    .requestMatchers("/api/**").authenticated() // Secure all /api REST endpoints
                    .anyRequest().denyAll() // Deny any other unmapped HTTP requests
            )
            .sessionManagement(sessionManagement ->
                sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .csrf(AbstractHttpConfigurer::disable)
            .oauth2ResourceServer(oauth2ResourceServer ->
                oauth2ResourceServer.jwt(jwt -> jwt.decoder(jwtDecoder()))
            );
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
    }

    @Bean // Added PasswordEncoder bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
