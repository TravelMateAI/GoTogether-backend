package com.example.Authservice.config;

import com.example.Authservice.service.UserserviceClient; // Corrected import
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.OidcClientInitiatedLogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(jsr250Enabled = true, prePostEnabled = true) // Enable method security for gRPC services
public class SecurityConfig {

    private final UserserviceClient userserviceClient; // Renamed
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri; // Needed for OidcClientInitiatedLogoutSuccessHandler


    public SecurityConfig(UserserviceClient userserviceClient, // Renamed
                          ClientRegistrationRepository clientRegistrationRepository) {
        this.userserviceClient = userserviceClient; // Renamed
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                    .requestMatchers("/actuator/**", "/login", "/login/**", "/oauth2/**", "/error",
                                     "/api/auth/register", "/api/auth/login", "/api/auth/token/refresh/local", // Permit local auth endpoints
                                     "/api/auth/logout_trigger") // Permit this specific logout trigger endpoint
                    .permitAll()
                    // /api/auth/perform_logout is Spring Security's processing URL, should be handled by its filter.
                    // Other /api/auth/** endpoints like /session, /user/me, /token/refresh/keycloak should require authentication.
                    .requestMatchers("/api/auth/**", "/api/users/me").authenticated()
                    .anyRequest().authenticated() // Default deny for anything else not specified above
            )
            .oauth2Login(oauth2Login ->
                oauth2Login
                    .loginPage("/login") // This is the Spring-default or custom page that initiates the flow
                    .defaultSuccessUrl("/api/auth/session", true) // After successful OIDC login, redirect here
                  .userInfoEndpoint(userInfoEndpoint ->
                     userInfoEndpoint.oidcUserService(this.oidcUserService())
                  )
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/perform_logout") // Spring Security will handle POST to this URL
                .logoutSuccessHandler(oidcLogoutSuccessHandler())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "SOME_OTHER_COOKIE_IF_ANY") // Specify cookies to clear
            )
            // If Authservice needs to validate its own locally issued JWTs for some REST endpoints (not currently planned)
            // another .oauth2ResourceServer(jwt -> jwt.jwtDecoder(localJwtDecoder())) chain for specific paths would be needed.
            // For gRPC, method security with the Keycloak JwtDecoder (from spring.security.oauth2.resourceserver.jwt.issuer-uri) will be used.
            .oauth2Client(withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public OidcUserService oidcUserService() {
        final OidcUserService delegate = new OidcUserService();
        return (OidcUserRequest userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            // Extract avatar URL (picture claim) if available
            String avatarUrl = oidcUser.getPicture();

            userserviceClient.ensureUserProfileExistsForOidcUser( // Use renamed client and method
                oidcUser.getSubject(),
                oidcUser.getPreferredUsername(),
                oidcUser.getEmail(),
                oidcUser.getGivenName(),
                oidcUser.getFamilyName(),
                avatarUrl // Pass avatar URL
            );
            return oidcUser;
        };
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        OidcClientInitiatedLogoutSuccessHandler successHandler = new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
        // Configure the post-logout redirect URI if desired and if Keycloak client is configured for it
        // String postLogoutRedirectUri = "http://localhost:8081/login?logout"; // Example
        // successHandler.setPostLogoutRedirectUri(postLogoutRedirectUri);
        return successHandler;
    }
}
