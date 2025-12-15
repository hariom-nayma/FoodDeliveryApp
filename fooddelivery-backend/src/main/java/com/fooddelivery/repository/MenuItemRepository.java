package com.fooddelivery.repository;

import com.fooddelivery.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, String> {
    
    @org.springframework.data.jpa.repository.Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId")
    List<MenuItem> findByRestaurantId(@org.springframework.data.repository.query.Param("restaurantId") String restaurantId);

    List<MenuItem> findByCategoryId(String categoryId);
}
