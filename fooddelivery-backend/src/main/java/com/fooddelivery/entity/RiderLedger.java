package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rider_ledger")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiderLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String riderId;

    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerType type;

    @Column(nullable = false)
    private Double amount; // Positive = Credit (Payable to Rider), Negative = Debit (Rider owes)

    private String description;

    @Builder.Default
    private boolean isSettled = false;

    private String settlementReference; // If paid out

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum LedgerType {
        EARNING, // Ride Pay
        COLLECTION, // Cash Collection (Negative)
        PAYOUT, // Bank Transfer (Negative or clearing balance)
        PENALTY, // Assessment (Negative)
        INCENTIVE // Bonus (Positive)
    }
}
