package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.CreatePostRequestDTO;
import com.example.socialmediaservice.dto.PostDTO;
import com.example.socialmediaservice.dto.PostsPageDTO;
import com.example.socialmediaservice.dto.UpdatePostRequestDTO;
import com.example.socialmediaservice.service.CommentService;
import com.example.socialmediaservice.service.PostService;
import com.example.socialmediaservice.service.ReactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final ReactionService reactionService;
    private final CommentService commentService;

    // 1. Create Post
    @PostMapping("/create")
    public ResponseEntity<PostDTO> createPost(
//            @PathVariable String userId,
            @RequestBody CreatePostRequestDTO createPostRequestDTO) {
        return ResponseEntity.ok(postService.createPost(createPostRequestDTO.getEmail(), createPostRequestDTO));
    }

    // 2. Get all posts created by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PostDTO>> getPostsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(postService.getPostsByUser(userId));
    }

    // 3. Get post by postId
    @GetMapping("/{postId}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable String postId) {
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    // 4. Update post content
    @PutMapping("/{postId}")
    public ResponseEntity<PostDTO> updatePost(
            @PathVariable String postId,
            @RequestBody UpdatePostRequestDTO requestDTO) {
        return ResponseEntity.ok(postService.updatePost(postId, requestDTO));
    }

    @GetMapping("/for-you")
    public ResponseEntity<PostsPageDTO> getForYouPosts(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(postService.getForYouFeed(cursor, size));
    }

}
