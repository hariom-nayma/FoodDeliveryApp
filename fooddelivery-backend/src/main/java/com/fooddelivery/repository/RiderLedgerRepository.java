package com.fooddelivery.repository;

import com.fooddelivery.entity.RiderLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RiderLedgerRepository extends JpaRepository<RiderLedger, String> {
    List<RiderLedger> findByRiderIdOrderByCreatedAtDesc(String riderId);

    List<RiderLedger> findByRiderIdAndIsSettledFalse(String riderId);

    @Query("SELECT SUM(l.amount) FROM RiderLedger l WHERE l.riderId = :riderId AND l.isSettled = false")
    Double getNetPayable(String riderId);
}
