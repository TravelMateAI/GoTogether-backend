package com.ISA.Restaurant.repo;

import com.ISA.Restaurant.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Notification> findByUserId(String userId);

    List<Notification> findByCreatedAtBefore(LocalDateTime twoHoursAgo);
}
