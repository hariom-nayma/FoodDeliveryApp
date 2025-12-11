package com.fooddelivery.repository;

import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.RestaurantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, String> {
    List<Restaurant> findByOwnerId(String ownerId);
    List<Restaurant> findByStatus(RestaurantStatus status);
    Optional<Restaurant> findByIdAndOwnerId(String id, String ownerId);
    boolean existsByPhone(String phone);
    @org.springframework.data.jpa.repository.Query("SELECT r FROM Restaurant r WHERE LOWER(r.address.city) LIKE LOWER(CONCAT('%', :city, '%')) AND r.status = :status")
    List<Restaurant> findByAddressCityAndStatus(@org.springframework.data.repository.query.Param("city") String city, @org.springframework.data.repository.query.Param("status") RestaurantStatus status);
}
