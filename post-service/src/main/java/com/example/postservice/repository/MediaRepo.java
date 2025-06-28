package com.example.postservice.repository;

import com.example.postservice.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepo extends JpaRepository<Media, String> {
}
