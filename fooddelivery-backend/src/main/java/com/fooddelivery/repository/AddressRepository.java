package com.fooddelivery.repository;

import com.fooddelivery.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserId(String userId);
    List<Address> findByUserIdAndIsDefaultTrue(String userId);
    List<Address> findAllByUserId(String userId);

    Optional<Address> findByIdAndUserId(String id, String userId);
}
