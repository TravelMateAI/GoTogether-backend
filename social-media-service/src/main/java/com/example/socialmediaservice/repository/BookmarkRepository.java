package com.example.socialmediaservice.repository;

import com.example.socialmediaservice.entity.Bookmark;
import com.example.socialmediaservice.entity.Post;
import com.example.socialmediaservice.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, String> {
    Optional<Bookmark> findByUserAndPost(User user, Post post);
    @Query("""
    SELECT b FROM Bookmark b
    WHERE b.user = :user
      AND (:cursor IS NULL OR b.createdAt < (SELECT b2.createdAt FROM Bookmark b2 WHERE b2.post.postId = :cursor))
    ORDER BY b.createdAt DESC
""")
    List<Bookmark> findBookmarksByUser(
            @Param("user") User user,
            @Param("cursor") String cursor,
            Pageable pageable
    );

    boolean existsByUserAndPost(User user, Post post);
}

