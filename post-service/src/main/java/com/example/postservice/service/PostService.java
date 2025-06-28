package com.example.postservice.service;


import com.example.postservice.dto.*;
import com.example.postservice.entity.*;
import com.example.postservice.enums.ReactionType;
import com.example.postservice.mapper.PostMapper;
import com.example.postservice.repository.PostRepo;
import com.example.postservice.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepo postRepo;
    private final UserRepo userRepo;
    private final ReactionService reactionService;
    private final CommentService commentService;
    private final BookmarkService bookmarkService;


    // 1. Create Post
    public CreatePostResponseDTO createPost(String email, CreatePostRequestDTO createPostRequestDTO) {
        log.info("email: {} | caption: {}", email, createPostRequestDTO.getCaption());

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setCaption(createPostRequestDTO.getCaption());
        post.setUser(user);

        // Link media if provided
        if (createPostRequestDTO.getMediaIds() != null && !createPostRequestDTO.getMediaIds().isEmpty()) {
            List<Media> mediaList = createPostRequestDTO.getMediaIds().stream()
                    .map(id -> {
                        Media media = new Media();
                        media.setId(id);
                        media.setPost(post); // Set the back-reference
                        return media;
                    })
                    .collect(Collectors.toList());
            post.setMediaList(mediaList);
        }

        postRepo.save(post);

        return PostMapper.toDto(post);
    }


    public List<PostDTO> getPostsByUser(String userId) {
        return postRepo.findByUser_UserId(userId).stream()
                .map(post -> {
                    PostDTO dto = PostMapper.toDto2(post);

                    UserDTO user = new UserDTO();
                    user.setId(post.getUser().getUserId());
                    user.setUsername(post.getUser().getUsername());
                    user.setDisplayName(post.getUser().getFirstName());
                    user.setAvatarUrl(post.getUser().getAvatarUrl());

                    // Add comments
                    dto.setComments(commentService.getCommentsByPost(post).stream().map(comment -> {
                        CommentDTO c = new CommentDTO();
                        c.setUser(user);
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


    public PostDTO getPostById(String postId) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) throw new RuntimeException("Post not found");

        PostDTO postDTO = PostMapper.toDto2(post);

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
        UserDTO userDTO = new UserDTO();
        userDTO.setId(comment.getUser().getUserId());
        userDTO.setUsername(comment.getUser().getUsername());
        userDTO.setDisplayName(comment.getUser().getFirstName());
        userDTO.setAvatarUrl(comment.getUser().getAvatarUrl());
        CommentDTO dto = new CommentDTO();
        dto.setUser(userDTO);
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



    //4. Update post content
    public PostDTO updatePost(String postId, UpdatePostRequestDTO requestDTO) {
        Post post = postRepo.findByPostId(postId);
        if (post == null) throw new RuntimeException("Post not found");

        post.setCaption(requestDTO.getCaption());

        // Replace media list if provided
        if (requestDTO.getMediaIds() != null) {
            List<Media> updatedMediaList = requestDTO.getMediaIds().stream()
                    .map(id -> {
                        Media media = new Media();
                        media.setId(id);
                        media.setPost(post);
                        return media;
                    })
                    .collect(Collectors.toList());
            post.setMediaList(updatedMediaList);
        }

        Post updatedPost = postRepo.save(post);
        return PostMapper.toDto2(updatedPost);
    }


    public PostsPageDTO getForYouFeed(String cursor, int size) {
        // Assuming cursor = postId of the last post from previous page
        List<Post> posts;

        if (cursor != null && !cursor.isBlank()) {
            posts = postRepo.findByPostIdLessThanOrderByCreatedAtDesc(cursor, PageRequest.of(0, size + 1));
        } else {
            posts = postRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size + 1));
        }

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        List<PostDTO> postDTOs = posts.stream().map(post -> {
            PostDTO dto = PostMapper.toDto2(post);

            // Get comments
            List<CommentDTO> commentDTOs = commentService.getCommentsByPost(post).stream().map(comment -> {
                UserDTO userDTO = new UserDTO();
                userDTO.setId(comment.getUser().getUserId());
                userDTO.setUsername(comment.getUser().getUsername());
                userDTO.setDisplayName(comment.getUser().getFirstName());
                userDTO.setAvatarUrl(comment.getUser().getAvatarUrl());

                CommentDTO commentDTO = new CommentDTO();
                commentDTO.setUser(userDTO);
                commentDTO.setContent(comment.getContent());
                commentDTO.setCreatedAt(comment.getCreatedAt());
                return commentDTO;
            }).collect(Collectors.toList());
            dto.setComments(commentDTOs);

            // Reactions
            Map<ReactionType, Long> reactionCounts = reactionService.getReactionCounts(post);
            dto.setReactionCounts(reactionCounts);

            // Set flat list of users who reacted
            dto.setReactions(post.getReactions() != null ?
                    post.getReactions().stream().map(reaction -> {
                        PostDTO.ReactionDTO r = new PostDTO.ReactionDTO();
                        r.setUserId(reaction.getUser().getUserId());
                        r.setReactionType(reaction.getType()); // Assuming Reaction entity has getType()
                        return r;
                    }).collect(Collectors.toList())
                    : List.of()
            );

            // Bookmarks (from bookmarkService, since Post doesn't have them)
            List<Bookmark> bookmarks = bookmarkService.getBookmarksByPost(post); // ← you must implement this
            dto.setBookmarks(bookmarks != null ?
                    bookmarks.stream().map(bookmark -> {
                        PostDTO.UserIdDTO b = new PostDTO.UserIdDTO();
                        b.setUserId(bookmark.getUser().getUserId());
                        return b;
                    }).collect(Collectors.toList())
                    : List.of()
            );

            PostDTO.CountDTO count = new PostDTO.CountDTO();
            count.setLikes(0);
            count.setComments(commentDTOs.size());
            dto.set_count(count);

            return dto;
        }).collect(Collectors.toList());

        PostsPageDTO page = new PostsPageDTO();
        page.setPosts(postDTOs);
        page.setNextCursor(hasNext ? postDTOs.get(postDTOs.size() - 1).getPostId() : null);

        return page;
    }

}
