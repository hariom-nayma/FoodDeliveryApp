package com.fooddelivery.service;

import com.fooddelivery.dto.request.AddressRequest;
import com.fooddelivery.dto.response.AddressResponse;
import com.fooddelivery.entity.Address;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.AddressRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // If default, unset others?
        if (request.isDefault()) {
            unsetDefaults(userId);
        }

        Address address = Address.builder()
                .user(user)
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPostalCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .label(request.getLabel())
                .isDefault(request.isDefault())
                .build();

        Address saved = addressRepository.save(address);
        return mapToResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(String userId, String addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        
        if (!address.getUser().getId().equals(userId)) {
             throw new RuntimeException("Unauthorized");
        }

        if (request.isDefault()) {
            unsetDefaults(userId);
        }

        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPostalCode());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setLabel(request.getLabel());
        address.setDefault(request.isDefault()); // Note: boolean getter isDefault, setter setDefault

        return mapToResponse(addressRepository.save(address));
    }

    public List<AddressResponse> getMyAddresses(String userId) {
        return addressRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAddress(String userId, String addressId) {
         Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!address.getUser().getId().equals(userId)) {
             throw new RuntimeException("Unauthorized");
        }
        addressRepository.delete(address);
    }

    private void unsetDefaults(String userId) {
        List<Address> defaults = addressRepository.findByUserIdAndIsDefaultTrue(userId);
        for (Address addr : defaults) {
            addr.setDefault(false);
            addressRepository.save(addr);
        }
    }

    private AddressResponse mapToResponse(Address a) {
        return AddressResponse.builder()
                .addressId(a.getId())
                .line1(a.getLine1())
                .line2(a.getLine2())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPincode())
                .latitude(a.getLatitude())
                .longitude(a.getLongitude())
                .label(a.getLabel())
                .isDefault(a.isDefault())
                .build();
    }
}
