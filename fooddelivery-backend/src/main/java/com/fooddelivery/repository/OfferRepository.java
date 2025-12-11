package com.fooddelivery.repository;

import com.fooddelivery.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OfferRepository extends JpaRepository<Offer, String> {
    Optional<Offer> findByCodeAndActiveTrue(String code);
}
