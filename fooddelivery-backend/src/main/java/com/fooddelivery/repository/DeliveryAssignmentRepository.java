package com.fooddelivery.repository;

import com.fooddelivery.entity.DeliveryAssignment;
import com.fooddelivery.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, String> {
    Optional<DeliveryAssignment> findByOrderAndStatus(Order order, String status);

    boolean existsByOrderAndStatus(com.fooddelivery.entity.Order order, String status);
    
    java.util.List<DeliveryAssignment> findByDeliveryPartner_IdAndStatus(String partnerId, String status);
}
