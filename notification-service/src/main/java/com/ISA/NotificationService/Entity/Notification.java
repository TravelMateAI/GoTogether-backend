package com.ISA.Restaurant.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId; // Can be customer ID or restaurant ID

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "type")
    private String type; // E.g., "order", "alert"

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public boolean setIsRead(boolean b) {
        this.isRead = b;
        return true;
    }

    public boolean getIsRead() {
        return isRead;
    }
}


