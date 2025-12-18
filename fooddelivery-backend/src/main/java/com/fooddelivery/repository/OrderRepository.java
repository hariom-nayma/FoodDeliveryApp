package com.fooddelivery.repository;

import com.fooddelivery.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    org.springframework.data.domain.Page<Order> findByUserIdOrderByCreatedAtDesc(String userId,
            org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Order> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId,
            org.springframework.data.domain.Pageable pageable);

    List<Order> findByUserIdAndStatusNotInOrderByCreatedAtDesc(String userId,
            java.util.Collection<com.fooddelivery.entity.OrderStatus> statuses);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id")
    java.util.Optional<Order> findByIdForUpdate(String id);
}
