package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String fileUrl;

    private String verificationStatus; // e.g. PENDING, VERIFIED, REJECTED
}
