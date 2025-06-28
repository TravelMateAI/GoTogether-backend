package com.example.Userservice.repository;

import com.example.Userservice.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Follow.FollowId> {

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    void deleteByFollowerId(String followerId); // For when a user is deleted
    void deleteByFollowingId(String followingId); // For when a user is deleted

    long countByFollowerId(String followerId); // How many others this user is following

    long countByFollowingId(String followingId); // How many followers this user has

    List<Follow> findByFollowerId(String followerId); // Get all Follow entities where user is the follower

    List<Follow> findByFollowingId(String followingId); // Get all Follow entities where user is being followed
}
