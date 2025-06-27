package com.ISA.Restaurant.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
//    private final ConcurrentHashMap<String, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        String userId = getUserIdFromSession(session);
//        sessionMap.put(userId, session);
//        System.out.println("WebSocket connection established for userId: " + userId);
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
//        String userId = getUserIdFromSession(session);
//        sessionMap.remove(userId);
//        System.out.println("WebSocket connection closed for userId: " + userId);
//    }
//
//    private String getUserIdFromSession(WebSocketSession session) {
//        // Extract userId from query parameters
//        return session.getUri().getQuery().split("=")[1];
//    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        log.info("NotificationWebSocketHandler afterConnectionEstablished: {}", uri);

        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        if ("userId".equals(key)) {
                            sessions.put(value, session);
                            log.info("New WebSocket connection established for user: {}", value);
                        }
                    }
                }
            } else {
                log.warn("No query parameters found in WebSocket connection.");
            }
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Received message from session {}: {}", session.getId(), message.getPayload());
        // Handle received messages if needed (e.g., acknowledgement or additional requests)
    }

//    @Override
//    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
//        log.info("WebSocket connection closed: {}", session.getId());
//        sessions.values().remove(session);
//    }

    public void sendNotification(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("Notification sent to userId {}: {}", userId, message);
            } catch (IOException e) {
                log.error("Failed to send notification to userId {}: {}", userId, e.getMessage());
            }
        } else {
            log.warn("No active WebSocket session found for userId: {}", userId);
        }
    }

    public void broadcastNotification(String message) {
        sessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.info("Broadcast notification sent to userId {}: {}", userId, message);
                } catch (IOException e) {
                    log.error("Failed to send broadcast notification to userId {}: {}", userId, e.getMessage());
                }
            }
        });
    }
}

