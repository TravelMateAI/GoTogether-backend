package com.example.postservice.repository;

import com.example.postservice.entity.Post;
import com.example.postservice.entity.Reaction;
import com.example.postservice.entity.User;
import com.example.postservice.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReactionRepo extends JpaRepository<Reaction, String> {
    Optional<Reaction> findByPostAndUser(Post post, User user);
    long countByPostAndType(Post post, ReactionType type);
}
