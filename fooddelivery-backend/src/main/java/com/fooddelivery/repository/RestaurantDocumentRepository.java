package com.fooddelivery.repository;

import com.fooddelivery.entity.RestaurantDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantDocumentRepository extends JpaRepository<RestaurantDocument, String> {
    List<RestaurantDocument> findByRestaurantId(String restaurantId);
}
