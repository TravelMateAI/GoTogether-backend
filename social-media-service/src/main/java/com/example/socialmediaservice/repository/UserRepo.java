package com.example.socialmediaservice.repository;

import com.example.socialmediaservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {

    Optional<User> findByUserId(String userId);
//    List<Post> findByUserProfile_PostId(String postId);
}
