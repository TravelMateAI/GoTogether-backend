package com.ISA.Restaurant.repo;

import com.ISA.Restaurant.Entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByEmailAndVerificationCode(String email, String verificationCode);
    Optional<Verification> findByEmail(String email);
    // Custom query to find expired verifications
    @Query("SELECT v FROM Verification v WHERE v.expiresAt < :threshold")
    List<Verification> findExpiredVerifications(LocalDateTime threshold);
}
