package com.example.Userservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "user_follows")
@Data
@NoArgsConstructor
@IdClass(Follow.FollowId.class) // Using IdClass for composite primary key
public class Follow {

    @Id
    @Column(name = "follower_id", nullable = false)
    private String followerId; // ID of the user who is following

    @Id
    @Column(name = "following_id", nullable = false)
    private String followingId; // ID of the user who is being followed

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Follow(String followerId, String followingId) {
        this.followerId = followerId;
        this.followingId = followingId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Composite Key Class
    public static class FollowId implements Serializable {
        private String followerId;
        private String followingId;

        public FollowId() {}

        public FollowId(String followerId, String followingId) {
            this.followerId = followerId;
            this.followingId = followingId;
        }

        // equals and hashCode are essential for composite keys
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FollowId followId = (FollowId) o;
            return Objects.equals(followerId, followId.followerId) &&
                   Objects.equals(followingId, followId.followingId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followingId);
        }
    }
}
