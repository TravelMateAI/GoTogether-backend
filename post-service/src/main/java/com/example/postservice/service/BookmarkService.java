package com.example.socialmediaservice.service;

import com.example.socialmediaservice.dto.CommentDTO;
import com.example.socialmediaservice.dto.PostDTO;
import com.example.socialmediaservice.dto.PostsPageDTO;
import com.example.socialmediaservice.dto.UserDTO;
import com.example.socialmediaservice.entity.Bookmark;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.mapper.PostMapper;
import com.example.socialmediaservice.repository.BookmarkRepository;
import com.example.socialmediaservice.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;
    private final UserRepo userRepo;
    private final CommentService commentService;
    private final ReactionService reactionService;

    public void toggleBookmark(User user, Post post) {
        bookmarkRepository.findByUserAndPost(user, post).ifPresentOrElse(
                bookmark -> bookmarkRepository.delete(bookmark),
                () -> bookmarkRepository.save(new Bookmark( user, post))
        );
    }

    public boolean isBookmarkedByUser(User user, Post post) {
        log.info("User: {} | Post: {}", user.getUserId(), post.getPostId());
        boolean isBookmarked = bookmarkRepository.existsByUserAndPost(user, post);
        log.info("Is bookmarked: {}", isBookmarked);
        return isBookmarked;
    }

    public PostsPageDTO getBookmarkedPosts(String userId, String cursor, int size) {
        User user = userRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(0, size + 1); // fetch one extra to check hasNext
        List<Bookmark> bookmarks = bookmarkRepository.findBookmarksByUser(user, cursor, pageable);

        List<Post> posts = bookmarks.stream()
                .map(Bookmark::getPost)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean hasNext = posts.size() > size;
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        List<PostDTO> postDTOs = posts.stream().map(post -> {
            PostDTO dto = PostMapper.toDto2(post); // Assuming similar to toDTO

            dto.setComments(commentService.getCommentsByPost(post).stream().map(comment -> {
                UserDTO userDTO = new UserDTO();
                userDTO.setId(comment.getUser().getUserId());
                userDTO.setUsername(comment.getUser().getUsername());
                userDTO.setDisplayName(comment.getUser().getFirstName());
                userDTO.setAvatarUrl(comment.getUser().getAvatarUrl());

                CommentDTO c = new CommentDTO();
                c.setUser(userDTO);
                c.setContent(comment.getContent());
                c.setCreatedAt(comment.getCreatedAt());
                return c;
            }).collect(Collectors.toList()));

            dto.setReactionCounts(reactionService.getReactionCounts(post));

            PostDTO.CountDTO count = new PostDTO.CountDTO();
            count.setLikes(0); // You can replace with actual counts if available
            count.setComments(0);
            dto.set_count(count);

            dto.setReactions(post.getReactions() != null ?
                    post.getReactions().stream().map(reaction -> {
                        PostDTO.ReactionDTO r = new PostDTO.ReactionDTO();
                        r.setUserId(reaction.getUser().getUserId());
                        r.setReactionType(reaction.getType()); // Assuming Reaction entity has getType()
                        return r;
                    }).collect(Collectors.toList())
                    : List.of()
            );

            return dto;
        }).collect(Collectors.toList());

        PostsPageDTO page = new PostsPageDTO();
        page.setPosts(postDTOs);
        page.setNextCursor(hasNext ? postDTOs.get(postDTOs.size() - 1).getPostId() : null);

        return page;
    }

    public List<Bookmark> getBookmarksByPost(Post post) {
        return bookmarkRepository.findAllByPost(post); // or a custom query
    }
}
