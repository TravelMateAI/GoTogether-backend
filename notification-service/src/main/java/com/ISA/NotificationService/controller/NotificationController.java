package com.ISA.Restaurant.controller;

import com.ISA.Restaurant.Entity.Notification;
import com.ISA.Restaurant.Entity.Restaurant;
import com.ISA.Restaurant.service.NotificationService;
import com.ISA.Restaurant.repo.RestaurantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final RestaurantRepository restaurantRepository;

    @Autowired
    public NotificationController(
            NotificationService notificationService,
            RestaurantRepository restaurantRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationService = notificationService;
        this.restaurantRepository = restaurantRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/notify")
    @SendTo("/topic/notifications")
    public String sendNotification(String message) {
        return "Notification: " + message;
    }

    public void sendOrderNotification(String userId, String orderId) {
        String destination = "/topic/notifications/" + userId;
        String message = "New order #" + orderId + " has been created.";
        messagingTemplate.convertAndSend(destination, message);
    }

    @PostMapping("/test-message")
    public ResponseEntity<?> sendTestMessage(@RequestParam String userId) {
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, "Test notification for user: " + userId);
        return ResponseEntity.ok("Test message sent.");
    }

    private Restaurant getAuthenticatedRestaurant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Authentication is null or user is not authenticated.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Restaurant) {
            String email = ((Restaurant) principal).getUsername();
            Optional<Restaurant> restaurantOpt = restaurantRepository.findByRestaurantEmail(email);
            return restaurantOpt.orElse(null);
        }

        log.warn("Unexpected principal type: {}", principal);
        return null;
    }

    @GetMapping("/restaurant-notifications")
    public ResponseEntity<List<Notification>> getNotificationsForAuthenticatedRestaurant() {
        Restaurant authenticatedRestaurant = getAuthenticatedRestaurant();
        if (authenticatedRestaurant == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        String restaurantId = String.valueOf(authenticatedRestaurant.getRestaurantId());
        List<Notification> notifications = notificationService.getNotificationsForUser(restaurantId);
        return ResponseEntity.ok(notifications);
    }

//    @GetMapping("/all")
//    public ResponseEntity<List<Notification>> getAllNotifications() {
//        List<Notification> notifications = notificationService.getAllNotifications();
//        return ResponseEntity.ok(notifications);
//    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotificationToUser(
            @RequestParam String userId,
            @RequestParam String message) {
        notificationService.sendNotificationToUser(userId, message);
        return ResponseEntity.ok("Notification sent to user: " + userId);
    }

    @PatchMapping("/mark-read/{notificationId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Notification marked as read.");
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead() {
        Restaurant authenticatedRestaurant = getAuthenticatedRestaurant();
        if (authenticatedRestaurant == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        String restaurantId = String.valueOf(authenticatedRestaurant.getRestaurantId());
        notificationService.markAllAsRead(restaurantId);
        return ResponseEntity.ok("All notifications marked as read.");
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<?> clearAllNotifications() {
        Restaurant authenticatedRestaurant = getAuthenticatedRestaurant();
        if (authenticatedRestaurant == null) {
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        String restaurantId = String.valueOf(authenticatedRestaurant.getRestaurantId());
        notificationService.clearAllNotifications(restaurantId);
        return ResponseEntity.ok("All notifications cleared.");
    }
}

