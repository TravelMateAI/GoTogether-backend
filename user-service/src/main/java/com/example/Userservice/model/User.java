package com.example.Userservice.model; // Renamed

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
// import java.util.Date; // For timestamps
// import jakarta.persistence.Temporal;
// import jakarta.persistence.TemporalType;
// import jakarta.persistence.PrePersist;
// import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id; // Keycloak User ID (subject claim)

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "hashed_password") // For locally registered users
    private String hashedPassword;

    @Column(name = "roles") // Comma-separated roles, e.g., "ROLE_USER,ROLE_ADMIN"
    private String roles;

    // Timestamps - uncomment if needed
    // @Column(name = "created_at", nullable = false, updatable = false)
    // @Temporal(TemporalType.TIMESTAMP)
    // private Date createdAt;

    // @Column(name = "updated_at", nullable = false)
    // @Temporal(TemporalType.TIMESTAMP)
    // private Date updatedAt;

    // @PrePersist
    // protected void onCreate() {
    //     createdAt = new Date();
    //     updatedAt = new Date();
    // }

    // @PreUpdate
    // protected void onUpdate() {
    //     updatedAt = new Date();
    // }
}
