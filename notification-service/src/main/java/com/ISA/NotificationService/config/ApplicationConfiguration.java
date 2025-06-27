package com.ISA.Restaurant.config;


import com.ISA.Restaurant.repo.RestaurantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ApplicationConfiguration {
    private final RestaurantRepository restaurantRepository; // Changed UserRepository to RestaurantRepository

    public ApplicationConfiguration(RestaurantRepository restaurantRepository) { // Changed UserRepository to RestaurantRepository
        this.restaurantRepository = restaurantRepository;
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> restaurantRepository.findByRestaurantEmail(username) // Changed userRepository to restaurantRepository
                .orElseThrow(() -> new UsernameNotFoundException("Restaurant not found")); // Changed "User not found" to "Restaurant not found"
    }

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }
}

