package com.example.socialmediaservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String userId;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = true)
    private String email;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();
}
