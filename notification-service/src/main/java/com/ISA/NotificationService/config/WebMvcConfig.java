package com.ISA.Restaurant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Exclude WebSocket paths from being handled as static resources
        registry.addResourceHandler("/ws/**")
                .addResourceLocations("classpath:/static/"); // Adjust if needed
    }
}
