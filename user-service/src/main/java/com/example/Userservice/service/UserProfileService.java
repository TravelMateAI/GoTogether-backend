package com.example.Userservice.service;

import com.example.Userservice.model.Follow;
import com.example.Userservice.model.User;
import com.example.Userservice.repository.FollowRepository;
import com.example.Userservice.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder; // Added

    @Autowired
    public UserProfileService(UserRepository userRepository, FollowRepository followRepository, PasswordEncoder passwordEncoder) { // Added
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.passwordEncoder = passwordEncoder; // Added
    }

    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public User createUser(User user) {
        logger.info("Attempting to create user: id={}, username={}", user.getUserId(), user.getUsername());
        if (userRepository.existsById(user.getUserId())) {
            logger.warn("User with ID {} already exists. Returning existing user.", user.getUserId());
            return userRepository.findById(user.getUserId())
                   .orElseThrow(() -> new IllegalStateException("User existed by ID but not found on immediate re-fetch for ID: " + user.getUserId()));
        }
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new DataIntegrityViolationException("Username '" + user.getUsername() + "' already exists.");
        });
        userRepository.findByEmail(user.getEmail()).ifPresent(u -> {
            throw new DataIntegrityViolationException("Email '" + user.getEmail() + "' already exists.");
        });

        return userRepository.save(user);
    }

    @Transactional
    public User updateUserProfile(String userId, String firstName, String lastName, String avatarUrl) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)); // Consider custom UserNotFoundException

        boolean changed = false;
        if (firstName != null && !firstName.equals(existingUser.getFirstName())) {
            existingUser.setFirstName(firstName);
            changed = true;
        }
        if (lastName != null && !lastName.equals(existingUser.getLastName())) {
            existingUser.setLastName(lastName);
            changed = true;
        }
        // Allow setting avatarUrl to empty string to clear it, or null to ignore.
        // If avatarUrl is an empty string, it implies clearing the avatar.
        // If it's null, it means no change to avatarUrl was requested via this parameter.
        if (avatarUrl != null) {
            if (!avatarUrl.equals(existingUser.getAvatarUrl())) {
                existingUser.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl); // Store null if empty string provided
                changed = true;
            }
        }

        if (changed) {
            logger.info("Updating profile for user {}", userId);
            return userRepository.save(existingUser);
        }
        logger.info("No changes detected for user profile {}", userId);
        return existingUser;
    }

    @Transactional
    public User updateAvatar(String userId, String avatarUrl) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)); // Consider custom UserNotFoundException

        String newAvatar = (avatarUrl != null && avatarUrl.isEmpty()) ? null : avatarUrl;
        if (!Objects.equals(existingUser.getAvatarUrl(), newAvatar)) {
            existingUser.setAvatarUrl(newAvatar);
            logger.info("Updating avatar for user {}", userId);
            return userRepository.save(existingUser);
        }
        logger.info("No change in avatar URL for user {}", userId);
        return existingUser;
    }

    @Transactional
    public void deleteUser(String userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)); // Consider custom UserNotFoundException

        logger.info("Deleting user with id: {}. Removing follow relationships.", userId);
        followRepository.deleteByFollowerId(userId);
        followRepository.deleteByFollowingId(userId);

        userRepository.delete(user);
        logger.info("Successfully deleted user with id: {}", userId);
    }

    // --- Follow Logic ---

    @Transactional
    public void followUser(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("User cannot follow themselves.");
        }
        // Ensure both users exist before attempting to create a follow relationship
        if (!userRepository.existsById(followerId)) {
             throw new IllegalArgumentException("Follower user with ID " + followerId +" not found.");
        }
         if (!userRepository.existsById(followingId)) {
             throw new IllegalArgumentException("User to follow with ID " + followingId + " not found.");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("User " + followerId + " is already following user " + followingId);
        }
        Follow follow = new Follow(followerId, followingId);
        followRepository.save(follow);
        logger.info("User {} started following user {}", followerId, followingId);
    }

    @Transactional
    public void unfollowUser(String followerId, String followingId) {
        if (!followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new IllegalStateException("User " + followerId + " is not following user " + followingId);
        }
        followRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
        logger.info("User {} unfollowed user {}", followerId, followingId);
    }

    @Transactional(readOnly = true)
    public List<User> getFollowers(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " not found when fetching followers.");
        }
        List<Follow> followerRelations = followRepository.findByFollowingId(userId);
        if (followerRelations.isEmpty()) return List.of();
        List<String> followerIds = followerRelations.stream().map(Follow::getFollowerId).collect(Collectors.toList());
        return userRepository.findAllById(followerIds);
    }

    @Transactional(readOnly = true)
    public List<User> getFollowing(String userId) {
         if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " not found when fetching following list.");
        }
        List<Follow> followingRelations = followRepository.findByFollowerId(userId);
        if (followingRelations.isEmpty()) return List.of();
        List<String> followingIds = followingRelations.stream().map(Follow::getFollowingId).collect(Collectors.toList());
        return userRepository.findAllById(followingIds);
    }

    @Data
    @AllArgsConstructor
    public static class FollowCounts {
        private long followerCount;
        private long followingCount;
    }

    @Transactional(readOnly = true)
    public FollowCounts getFollowCounts(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User with ID " + userId + " not found when fetching follow counts.");
        }
        long followerCount = followRepository.countByFollowingId(userId);
        long followingCount = followRepository.countByFollowerId(userId);
        return new FollowCounts(followerCount, followingCount);
    }

    // --- Methods for Local Account Management by Authservice ---

    @Transactional
    public User createLocalAccount(String username, String email, String plainPassword, String roles) {
        logger.info("Attempting to create local account: username={}, email={}", username, email);
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DataIntegrityViolationException("Username '" + username + "' already exists.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DataIntegrityViolationException("Email '" + email + "' already exists.");
        }

        User newUser = new User();
        // For local accounts, ID could be auto-generated (UUID) if not conflicting with Keycloak's 'sub' format.
        // Or Authservice could generate an ID and pass it.
        // For now, let's assume Userservice generates it if not provided, or Authservice passes a compatible one.
        // If using UUIDs for local users and Keycloak 'sub' for others, ID field needs to accommodate both.
        // For simplicity, assuming ID is set before this call or handled by User entity's @GeneratedValue if applicable.
        // If ID is passed from Authservice (e.g. a new UUID generated by Authservice), it should be set on newUser.
        // If ID is auto-generated by DB, then it will be set after save.
        // Here, we assume ID will be set if it's a required field from CreateLocalAccountRequest in gRPC.
        // Let's assume CreateLocalAccountRequest in gRPC layer will require a userId to be passed, or Userservice creates one.
        // For this method, we'll assume the User object passed to createUser will have an ID.
        // The gRPC layer will be responsible for creating the User object with an ID.
        // This service method is more about the core logic.

        newUser.setUsername(username);
        newUser.setEmail(email);
        java.util.UUID uuid = java.util.UUID.randomUUID();
        newUser.setUserId("local-" + uuid.toString()); // Prefix to distinguish from Keycloak IDs

        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticateLocalAccount(String username, String plainPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
        }
        return Optional.empty();
    }
}
