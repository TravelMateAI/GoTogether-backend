package com.ISA.Restaurant.controller;

public interface NotificationSender {
    void sendOrderNotification(String userId, String message);
}
