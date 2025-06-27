package com.ISA.Restaurant.service;


import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {
    private ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void registerSession(String customerId, WebSocketSession session) {
        sessions.put(customerId, session);
    }

    public void sendLocationUpdate(String customerId, double latitude, double longitude) {
        WebSocketSession session = sessions.get(customerId);
        if (session != null && session.isOpen()) {
            try {
                String locationJson = String.format("{\"latitude\": %f, \"longitude\": %f}", latitude, longitude);
                session.sendMessage(new TextMessage(locationJson));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            System.out.println(sessions);
        }
    }
}

