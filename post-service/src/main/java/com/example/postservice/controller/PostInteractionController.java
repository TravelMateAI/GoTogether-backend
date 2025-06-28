package com.example.postservice.controller;

import com.example.postservice.dto.*;
import com.example.postservice.entity.Comment;
import com.example.postservice.entity.Post;
import com.example.postservice.entity.User;
import com.example.postservice.enums.ReactionType;
import com.example.postservice.repository.CommentRepo;
import com.example.postservice.repository.PostRepo;
import com.example.postservice.repository.UserRepo;
import com.example.postservice.service.BookmarkService;
import com.example.postservice.service.CommentService;
import com.example.postservice.service.ReactionService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(
        origins = {"http://localhost:3000", "https://go-together-uom.vercel.app"},
        allowCredentials = "true"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostInteractionController {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final CommentRepo commentRepo;
    private final CommentService commentService;
    private final ReactionService reactionService;
    private final BookmarkService bookmarkService;

    // 1. Get all comments for a specific post
    @GetMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> getCommentsForPost(
            @PathVariable String postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Comment> commentPage = commentRepo.findByPost(post, pageable);

        List<CommentDTO> commentDTOs = commentPage.stream().map(comment -> {
            CommentDTO dto = new CommentDTO();
            UserDTO userDTO = new UserDTO();
            userDTO.setId(comment.getUser().getUserId());
            userDTO.setUsername(comment.getUser().getUsername());
            userDTO.setDisplayName(comment.getUser().getFirstName());
            userDTO.setAvatarUrl(comment.getUser().getAvatarUrl());
            dto.setCommentId(comment.getCommentId());
            dto.setUser(userDTO);
            dto.setContent(comment.getContent());
            dto.setCreatedAt(comment.getCreatedAt());
            return dto;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentDTOs);
        response.put("hasNext", commentPage.hasNext());
        response.put("nextPage", page + 1);

        return ResponseEntity.ok(response);
    }


    // 2. Get reaction counts for a specific post
    @GetMapping("/{postId}/reactions")
    public ResponseEntity<Map<ReactionType, Long>> getReactionCounts(@PathVariable String postId) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        Map<ReactionType, Long> counts = reactionService.getReactionCounts(post);
        return ResponseEntity.ok(counts);
    }

    // 3. Add reactions for a post
    @PostMapping("/{postId}/react")
    public ResponseEntity<Void> addOrUpdateReaction(@PathVariable String postId,
                                                    @RequestParam String userId,
                                      @RequestParam ReactionType type) {

        Post post = postRepo.findByPostId(postId);
        if (post == null) return ResponseEntity.notFound().build();
        System.out.println("UserID received: " + userId);
        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reactionService.reactToPost(post, user, type);
        return ResponseEntity.ok().build();
    }

    // 4. Add comments to a post
    @PostMapping("{postId}/comment")
    public ResponseEntity<Void> addCommentToPost(@PathVariable String postId,
                                                 @RequestBody RequestCommentDTO requestDTO) {
        log.info("PostId: {}, User: {}", postId, requestDTO.getUserId());
        Post post = postRepo.findByPostId(postId);

        if (post == null) return ResponseEntity.notFound().build();

        User user = userRepo.findByUserId(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        commentService.addComment(post, user, requestDTO.getContent());
        return ResponseEntity.ok().build();
    }

    // 5. Reply to a comment on a post
    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<Void> replyToComment(@PathVariable String commentId,
                                               @RequestBody ReplyCommentRequestDTO requestDTO) {
        Comment parentComment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        User user = userRepo.findByUserId(requestDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = parentComment.getPost();

        commentService.replyToComment(parentComment, post, user, requestDTO.getContent());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String commentId,
            @RequestParam String userId
    ) {
        log.info("CommentId: {}, UserId: {}", commentId, userId);
        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

//        log.info("Comment: {}", comment.toString());

        if (!comment.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        commentRepo.delete(comment);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{postId}/bookmark")
    public ResponseEntity<Void> addBookmark(@PathVariable String postId, @RequestParam String userId) {
        log.info("PostId: {}, UserId: {}", postId, userId);
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + postId));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        bookmarkService.toggleBookmark(user, post);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{postId}/bookmark")
    public ResponseEntity<Void> removeBookmark(@PathVariable String postId, @RequestParam String userId) {
        return addBookmark(postId, userId); // toggle logic is same
    }

    @GetMapping("/{postId}/bookmark")
    public ResponseEntity<Map<String, Boolean>> checkBookmark(@PathVariable String postId, @RequestParam String userId) {
        Post post = postRepo.findByPostId(postId);
        log.info("Post id {}", postId);
        if (post == null) {
            throw new RuntimeException("Post not found");
        }
        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        boolean isBookmarked = bookmarkService.isBookmarkedByUser(user, post);
        return ResponseEntity.ok(Map.of("isBookmarkedByUser", isBookmarked));
    }

    @GetMapping("/bookmarked")
    public ResponseEntity<PostsPageDTO> getBookmarkedPosts(
            @RequestParam String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size) {

        log.info(" Fetch bookmarked posts | UserId: {}, Cursor: {}, Size: {}", userId, cursor, size);
        PostsPageDTO result = bookmarkService.getBookmarkedPosts(userId, cursor, size);
        return ResponseEntity.ok(result);
    }
}
