package com.ISA.Restaurant.config;

import com.ISA.Restaurant.service.CustomerLocationWebSocketHandler;

import com.ISA.Restaurant.service.handler.NotificationWebSocketHandler;
import com.ISA.Restaurant.utils.CustomHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notification-stomp")
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173")
                .withSockJS()
                .setInterceptors(new CustomHandshakeInterceptor());
    }
}


//@Configuration
//@EnableWebSocket
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketConfigurer {
//
//    private final CustomerLocationWebSocketHandler customerLocationWebSocketHandler;
//    private final NotificationWebSocketHandler notificationWebSocketHandler;
//
//    @Autowired
//    public WebSocketConfig(
//            CustomerLocationWebSocketHandler customerLocationWebSocketHandler,
//            NotificationWebSocketHandler notificationWebSocketHandler) {
//        this.customerLocationWebSocketHandler = customerLocationWebSocketHandler;
//        this.notificationWebSocketHandler = notificationWebSocketHandler;
//    }
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic"); // Enable in-memory broker
//        config.setApplicationDestinationPrefixes("/app"); // Prefix for client-to-server messages
//    }
//
//    @Override
//    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        // Customer-location WebSocket endpoint
//        registry.addHandler(customerLocationWebSocketHandler, "/ws/Customer-location")
//                .setAllowedOrigins("*");
//
//        // Notification WebSocket endpoint
//        registry.addHandler(notificationWebSocketHandler, "/ws/notification")
//                .setAllowedOrigins("*");
////                .setAllowedOriginPatterns("http://localhost:5173", "http://example.com")
////                .withSockJS();
//    }
//}

