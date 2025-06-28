package com.example.postservice.repository;


import com.example.postservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);
    Optional<User> findByEmail(String username);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
//    List<Post> findByUserProfile_PostId(String postId);
}
