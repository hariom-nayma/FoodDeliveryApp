package com.fooddelivery.repository;

import com.fooddelivery.entity.DeliveryPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, String> {
    Optional<DeliveryPartner> findByUserId(String userId);

    List<DeliveryPartner> findByStatus(String status);
}
