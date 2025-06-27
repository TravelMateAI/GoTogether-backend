package com.example.socialmediaservice.service;

import com.example.socialmediaservice.dto.FollowerInfo;
import com.example.socialmediaservice.dto.UpdateProfileRequest;
import com.example.socialmediaservice.dto.UpdateProfileResponse;
import com.example.socialmediaservice.entity.User;
import com.example.socialmediaservice.repository.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepo userRepository;
    private final WebClient.Builder webClientBuilder;

    public User updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setAvatarUrl(avatarUrl);
        return userRepository.save(user);
    }


    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
        user.setPosts(null);
        return user;
    }
    public User getUserById(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
        user.setPosts(null);
        return user;
    }


    public UpdateProfileResponse updateUserProfile(String userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (request.getDisplayName() != null) {
            user.setFirstName(request.getDisplayName()); // ✅ assuming 'firstName' is your displayName
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        User saved = userRepository.save(user);
        return new UpdateProfileResponse(
                saved.getUserId(),
                saved.getUsername(),
                saved.getFirstName(),
                saved.getAvatarUrl(),
                saved.getBio()
        );
    }


    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));
    }

    @Transactional
    public void followUser(String followerId, String targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        User follower = userRepository.findByUserId(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean added = follower.getFollowingIds().add(targetUserId);
        target.getFollowerIds().add(followerId);

        log.info("Added: {} | Target: {}", added, targetUserId);
        log.info("Follower: {} | Target: {}", followerId, targetUserId);
        log.info("Follower: {} | Target: {}", follower.getFollowingIds().toString(), target.getFollowerIds().toString() );

        if (added) {
            userRepository.save(follower);
            userRepository.save(target);
        }
    }

    @Transactional
    public void unfollowUser(String followerId, String targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot unfollow yourself.");
        }

        User follower = userRepository.findByUserId(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean removedFromFollowing = follower.getFollowingIds().remove(targetUserId);
        boolean removedFromFollowers = target.getFollowerIds().remove(followerId);
        log.info("RemovedFromFollowing: {} | RemovedFromFollowers: {}", removedFromFollowing, removedFromFollowers);
        log.info("Follower: {} | Target: {}", followerId, targetUserId);
        log.info("Follower: {} | Target: {}", follower.getFollowingIds().toString(), target.getFollowerIds().toString() );

        if (removedFromFollowing || removedFromFollowers) {
            userRepository.save(follower);
            userRepository.save(target);
        }
    }


    public FollowerInfo getFollowerInfo(String currentUserId, String targetUserId) {
        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        boolean isFollowing = target.getFollowerIds().contains(currentUserId);
        int count = target.getFollowerIds().size();

        return new FollowerInfo(count, isFollowing);
    }

}
