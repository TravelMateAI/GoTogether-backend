package com.example.socialmediaservice.repository;

import com.example.socialmediaservice.entity.Bookmark;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    Optional<Bookmark> findByUserAndPost(User user, Post post);
    boolean existsByUserAndPost(User user, Post post);
}

