package com.example.socialmediaservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Column(nullable = true, columnDefinition = "TEXT")
    private String bio;

    @Column(nullable = true)
    private String avatarUrl;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("user") // Prevent recursion during serialization
    private List<Post> posts = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "followers",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "follower_id")
    )
    @JsonIgnoreProperties("followers") // Prevent full circular graph
    private Set<User> followers = new HashSet<>();

    @ManyToMany(mappedBy = "followers")
    @JsonIgnoreProperties("following")
    private Set<User> following = new HashSet<>();

}
