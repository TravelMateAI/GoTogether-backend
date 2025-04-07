package com.example.socialmediaservice.controller;

import com.example.socialmediaservice.dto.CommentDTO;
import com.example.socialmediaservice.dto.ReplyCommentRequestDTO;
import com.example.socialmediaservice.entity.Comment;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.enums.ReactionType;
import com.example.socialmediaservice.repository.CommentRepo;
import com.example.socialmediaservice.repository.PostRepo;
import com.example.socialmediaservice.repository.UserRepo;
import com.example.socialmediaservice.service.CommentService;
import com.example.socialmediaservice.service.ReactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostInteractionController {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final CommentRepo commentRepo;
    private final CommentService commentService;
    private final ReactionService reactionService;

    // 1. Get all comments for a specific post
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsForPost(
            @PathVariable String postId) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) {
            return ResponseEntity.notFound().build();
        }

        List<CommentDTO> commentDTOs = commentService.getCommentsByPost(post).stream().map(comment -> {
            CommentDTO dto = new CommentDTO();
            dto.setUserId(comment.getUser().getUserId());
            dto.setContent(comment.getContent());
            dto.setCreatedAt(comment.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(commentDTOs);
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

        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        reactionService.reactToPost(post, user, type);
        return ResponseEntity.ok().build();
    }

    // 4. Add comments to a post
    @PostMapping("{postId}/comment")
    public ResponseEntity<Void> addCommentToPost(@PathVariable String postId,
                                                 @RequestBody CommentDTO requestDTO) {
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



}
