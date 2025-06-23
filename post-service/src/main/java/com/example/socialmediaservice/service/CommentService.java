package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.Comment;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.CommentRepo;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepo;

    public Comment addComment(Post post, User user, String content, Comment parentComment) {
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setParentComment(parentComment);
        return commentRepo.save(comment);
    }

    public Comment addComment(Post post, User user, String content) {
        return addComment(post, user, content, null);
    }

    public List<Comment> getCommentsByPost(Post post) {
        return commentRepo.findByPost(post);
    }

    public List<Comment> getRepliesByParentComment(Comment parentComment) {
        return commentRepo.findByParentComment(parentComment);
    }

    public Comment replyToComment(Comment parentComment, Post post, User user, String content) {
        Comment reply = new Comment();
        reply.setParentComment(parentComment);
        reply.setPost(post);
        reply.setUser(user);
        reply.setContent(content);
        return commentRepo.save(reply);
    }

}
