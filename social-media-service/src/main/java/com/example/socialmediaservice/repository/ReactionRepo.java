package com.example.socialmediaservice.repository;

import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.Reaction;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepo extends JpaRepository<Reaction, String> {
    Optional<Reaction> findByPostAndUser(Post post, User user);
    long countByPostAndType(Post post, ReactionType type);
}
