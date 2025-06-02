package com.example.socialmediaservice.service;

import com.example.socialmediaservice.dto.PostDTO;
import com.example.socialmediaservice.dto.PostsPageDTO;
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

        Pageable pageable = PageRequest.of(0, size);
        List<Bookmark> bookmarks = bookmarkRepository.findBookmarksByUser(user, cursor, pageable);

        List<PostDTO> validPosts = bookmarks.stream()
                .map(Bookmark::getPost)
                .filter(Objects::nonNull)
                .map(PostMapper::toDTO)
                .toList();

        String nextCursor = validPosts.size() < size ? null
                : validPosts.get(validPosts.size() - 1).getPostId();

        PostsPageDTO response = new PostsPageDTO();
        response.setPosts(validPosts);
        response.setNextCursor(nextCursor);
        return response;
    }

}
