package com.example.socialmediaservice.repository;

import com.example.socialmediaservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepo extends JpaRepository<Post, String> {
    List<Post> findByUser_UserId(String userId);
    Post findByPostId(String postId);
}
