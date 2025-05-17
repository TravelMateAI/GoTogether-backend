package com.example.socialmediaservice.service;

import com.example.socialmediaservice.entity.Bookmark;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

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
}
