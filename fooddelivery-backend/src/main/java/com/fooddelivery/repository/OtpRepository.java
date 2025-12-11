package com.fooddelivery.repository;

import com.fooddelivery.entity.Otp;
import com.fooddelivery.entity.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, String> {
    Optional<Otp> findByEmailAndTypeAndUsedFalse(String email, OtpType type); // Keep for compatibility if needed, but
                                                                              // we should rely on findTop

    java.util.List<Otp> findAllByEmailAndTypeAndUsedFalse(String email, OtpType type);

    Optional<Otp> findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(String email, OtpType type);
}
