package com.example.postservice.repository;

import com.example.postservice.entity.Comment;
import com.example.postservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepo extends JpaRepository<Comment, String> {
    List<Comment> findByPost(Post post);
    List<Comment> findByPostAndParentCommentIsNull(Post post);
    Page<Comment> findByPost(Post post, Pageable pageable);
    List<Comment> findByParentComment(Comment parentComment);
}
