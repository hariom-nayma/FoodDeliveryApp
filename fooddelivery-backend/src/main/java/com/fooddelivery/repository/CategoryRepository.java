package com.fooddelivery.repository;

import com.fooddelivery.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    List<Category> findByRestaurantIdOrderBySortOrderAsc(String restaurantId);
    List<Category> findByRestaurantIdAndActiveOrderBySortOrderAsc(String restaurantId, boolean active);
    Optional<Category> findByIdAndRestaurantId(String id, String restaurantId);
}
