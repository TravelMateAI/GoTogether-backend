package com.example.socialmediaservice.service;


import com.example.socialmediaservice.dto.CommentDTO;
import com.example.socialmediaservice.dto.CreatePostRequestDTO;
import com.example.socialmediaservice.dto.PostDTO;
import com.example.socialmediaservice.dto.UpdatePostRequestDTO;
import com.example.socialmediaservice.entity.Comment;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.enums.ReactionType;
import com.example.socialmediaservice.mapper.PostMapper;
import com.example.socialmediaservice.repository.PostRepo;
import com.example.socialmediaservice.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final ReactionService reactionService;
    private final CommentService commentService;


    // 1. Create Post
    public PostDTO createPost(String userId, CreatePostRequestDTO createPostRequestDTO) {
        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setContent(createPostRequestDTO.getContent());
        post.setUser(user);
        postRepo.save(post);

        return PostMapper.toDto(post);
    }

    // 2. Get all posts created by a specific user

//    public List<PostDTO> getPostsByUser(String userId) {
//        return postRepo.findByUser_UserId(userId).stream()
//                .map(PostMapper::toDto)
//                .collect(Collectors.toList());
//    }

    public List<PostDTO> getPostsByUser(String userId) {
        return postRepo.findByUser_UserId(userId).stream()
                .map(post -> {
                    PostDTO dto = PostMapper.toDto(post);

                    // Add comments
                    dto.setComments(commentService.getCommentsByPost(post).stream().map(comment -> {
                        CommentDTO c = new CommentDTO();
                        c.setUserId(comment.getUser().getUserId());
                        c.setContent(comment.getContent());
                        c.setCreatedAt(comment.getCreatedAt());
                        return c;
                    }).collect(Collectors.toList()));

                    // Add reaction counts
                    dto.setReactionCounts(reactionService.getReactionCounts(post));

                    return dto;
                })
                .collect(Collectors.toList());
    }


    // 3. Get post by ID with comments and reaction counts
//    public PostDTO getPostById(String postId) {
//        Post post = postRepo.findByPostId(postId);
//        if (post == null) throw new RuntimeException("Post not found");
//
//        PostDTO postDTO = PostMapper.toDto(post);
//
//        // Fetch and map comments
//        List<Comment> comments = commentService.getCommentsByPost(post);
//        List<CommentDTO> commentDTOs = comments.stream().map(comment -> {
//            CommentDTO dto = new CommentDTO();
//            dto.setUserId(comment.getUser().getUserId()); // Assumes getUserId exists
//            dto.setContent(comment.getContent());
//            dto.setCreatedAt(comment.getCreatedAt());
//            dto.setParentCommentId(comment.getParentComment().getCommentId());
//            List<Comment> replies = commentService.getRepliesByParentComment(comment);
//            return dto;
//        }).collect(Collectors.toList());
//        postDTO.setComments(commentDTOs);
//
//        // Get reaction counts
//        Map<ReactionType, Long> reactionCounts = reactionService.getReactionCounts(post);
//        postDTO.setReactionCounts(reactionCounts);
//
//        return postDTO;
//    }

    public PostDTO getPostById(String postId) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) throw new RuntimeException("Post not found");

        PostDTO postDTO = PostMapper.toDto(post);

        // Fetch all comments for the post
        List<Comment> allComments = commentService.getCommentsByPost(post);

        // Group comments by parent
        Map<String, List<Comment>> repliesGroupedByParentId = allComments.stream()
                .filter(c -> c.getParentComment() != null)
                .collect(Collectors.groupingBy(c -> c.getParentComment().getCommentId()));

        // Filter top-level comments
        List<Comment> topLevelComments = allComments.stream()
                .filter(c -> c.getParentComment() == null)
                .collect(Collectors.toList());

        // Map top-level comments and recursively add replies
        List<CommentDTO> commentDTOs = topLevelComments.stream()
                .map(comment -> mapCommentWithReplies(comment, repliesGroupedByParentId))
                .collect(Collectors.toList());

        postDTO.setComments(commentDTOs);

        // Set reaction counts
        Map<ReactionType, Long> reactionCounts = reactionService.getReactionCounts(post);
        postDTO.setReactionCounts(reactionCounts);

        return postDTO;
    }

    private CommentDTO mapCommentWithReplies(Comment comment, Map<String, List<Comment>> repliesGroupedByParentId) {
        CommentDTO dto = new CommentDTO();
        dto.setUserId(comment.getUser().getUserId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setParentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null);

        List<Comment> replies = repliesGroupedByParentId.get(comment.getCommentId());
        if (replies != null && !replies.isEmpty()) {
            List<CommentDTO> replyDTOs = replies.stream()
                    .map(reply -> mapCommentWithReplies(reply, repliesGroupedByParentId)) // Recursive
                    .collect(Collectors.toList());
            dto.setReplies(replyDTOs);
        }

        return dto;
    }



//    //4. Get post by ID
//    public PostDTO getPostById(String postId) {
//        Post post = postRepo.findByPostId(postId);
//        return PostMapper.toDto(post);
//    }

    //4. Update post content
    public PostDTO updatePost(String postId, UpdatePostRequestDTO requestDTO) {
        Post post = postRepo.findByPostId(postId);
        post.setContent(requestDTO.getContent());
        Post updatedPost = postRepo.save(post);
        return PostMapper.toDto(updatedPost);
    }

}
