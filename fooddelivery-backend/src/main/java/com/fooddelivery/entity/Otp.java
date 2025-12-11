package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Otp extends BaseEntity {

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;
}
