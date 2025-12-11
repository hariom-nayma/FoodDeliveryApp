package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId);

    List<Order> findByUserIdAndStatusNotInOrderByCreatedAtDesc(String userId,
            java.util.Collection<com.fooddelivery.entity.OrderStatus> statuses);
}
