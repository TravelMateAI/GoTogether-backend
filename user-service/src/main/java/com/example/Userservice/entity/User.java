//package com.example.Userservice.entity;
//
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//
////@Entity
//@Getter
//@Setter
////@ToString(exclude = {"followers", "following"})
////@EqualsAndHashCode(onlyExplicitlyIncluded = true)
////@Table(name = "users")
//public class User {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    private String userId;
//
//    @Column(unique = true, nullable = false)
//    private String username;
//
//    @Column(unique = true, nullable = true)
//    private String email;
//
//    @Column(nullable = true)
//    private String firstName;
//
//    @Column(nullable = true)
//    private String lastName;
//
//    @Column(nullable = true, columnDefinition = "TEXT")
//    private String bio;
//
//    @Column(nullable = true)
//    private String avatarUrl;
//
////    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
////    @JsonIgnoreProperties("user") // Prevent recursion during serialization
////    private List<Post> posts = new ArrayList<>();
//
//    @ElementCollection
//    @CollectionTable(name = "user_followers", joinColumns = @JoinColumn(name = "user_id"))
//    @Column(name = "follower_user_id")
//    private Set<String> followerIds = new HashSet<>();
//
//    @ElementCollection
//    @CollectionTable(name = "user_following", joinColumns = @JoinColumn(name = "user_id"))
//    @Column(name = "following_user_id")
//    private Set<String> followingIds = new HashSet<>();
//
//    @ElementCollection
//    @CollectionTable(name = "user_places", joinColumns = @JoinColumn(name = "user_id"))
//    @Column(name = "place")
//    private List<String> places = new ArrayList<>();
//
//}
//
