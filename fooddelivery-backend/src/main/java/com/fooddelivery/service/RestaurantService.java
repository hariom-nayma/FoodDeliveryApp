package com.fooddelivery.service;

import com.fooddelivery.dto.request.DocumentUploadRequest;
import com.fooddelivery.dto.request.RestaurantRequest;
import com.fooddelivery.dto.request.RestaurantStatusUpdateRequest;
import com.fooddelivery.dto.response.RestaurantResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.RestaurantDocumentRepository;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final com.fooddelivery.repository.OrderRepository orderRepository;

    public List<Order> getOrders(String restaurantId) {
        return orderRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId);
    }

    @Transactional
    public RestaurantResponse createRestaurant(RestaurantRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (restaurantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Restaurant phone already registered");
        }

        RestaurantAddress address = RestaurantAddress.builder()
                .addressLine1(request.getAddress().getAddressLine1())
                .city(request.getAddress().getCity())
                .state(request.getAddress().getState())
                .pincode(request.getAddress().getPincode())
                .latitude(request.getAddress().getLatitude())
                .longitude(request.getAddress().getLongitude())
                .build();

        Restaurant restaurant = Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .phone(request.getPhone())
                .email(request.getEmail())
                .cuisineTypes(request.getCuisineTypes())
                .address(address)
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .status(RestaurantStatus.DRAFT)
                .owner(owner)
                .build();

        Restaurant saved = restaurantRepository.save(restaurant);
        return mapToResponse(saved);
    }

    @Transactional
    public void uploadDocuments(String restaurantId, DocumentUploadRequest request, String ownerEmail) {
        Restaurant restaurant = getRestaurantIfOwner(restaurantId, ownerEmail);

        restaurant.setGstNumber(request.getGstNumber());
        restaurant.setFssaiNumber(request.getFssaiNumber());
        restaurantRepository.save(restaurant);

        // Clear existing docs if any or append? usually append or replace specific types.
        // For simplicity, let's just add new ones.
        java.util.Set<String> uploadedTypes = new java.util.HashSet<>();
        
        for (DocumentUploadRequest.DocumentDto docDto : request.getDocuments()) {
            RestaurantDocument doc = RestaurantDocument.builder()
                    .restaurant(restaurant)
                    .type(DocumentType.valueOf(docDto.getType()))
                    .fileUrl(docDto.getFileUrl())
                    .verificationStatus("PENDING") // Initial status
                    .build();
            documentRepository.save(doc);
            uploadedTypes.add(docDto.getType());
        }

        // Auto-submit if all mandatory docs are present
        if (uploadedTypes.contains("GST_CERTIFICATE") && 
            uploadedTypes.contains("FSSAI_LICENSE") && 
            uploadedTypes.contains("PAN_CARD")) {
            
            if (restaurant.getStatus() == RestaurantStatus.DRAFT) {
                restaurant.setStatus(RestaurantStatus.PENDING_REVIEW);
                restaurantRepository.save(restaurant);
            }
        }
    }

    @Transactional
    public RestaurantResponse submitForReview(String restaurantId, String ownerEmail) {
        Restaurant restaurant = getRestaurantIfOwner(restaurantId, ownerEmail);
        
        // Validation: Check if docs exist? (Optional for now)

        restaurant.setStatus(RestaurantStatus.PENDING_REVIEW);
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public RestaurantResponse approveRestaurant(String restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        restaurant.setStatus(RestaurantStatus.APPROVED);
        
        // Upgrade user role to OWNER
        User owner = restaurant.getOwner();
        if (owner.getRole() == Role.ROLE_CUSTOMER) {
             owner.setRole(Role.ROLE_RESTAURANT_OWNER);
             userRepository.save(owner);
        }
        
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public RestaurantResponse rejectRestaurant(String restaurantId, String reason) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        restaurant.setStatus(RestaurantStatus.REJECTED);
        // Log reason if we had a history table
        
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    public List<RestaurantResponse> getPendingRestaurants() {
        return restaurantRepository.findByStatus(RestaurantStatus.PENDING_REVIEW).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public RestaurantResponse getRestaurant(String id) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return mapToResponse(restaurant);
    }

    public List<RestaurantResponse> searchRestaurants(String city) {
        return restaurantRepository.findByAddressCityAndStatus(city, RestaurantStatus.APPROVED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RestaurantResponse updateStatus(String restaurantId, String statusStr, String ownerEmail, boolean isAdmin) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        
        if (!isAdmin && !restaurant.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Access denied");
        }

        RestaurantStatus newStatus = RestaurantStatus.valueOf(statusStr);
        // Owner checks
        if (!isAdmin) {
            if (newStatus != RestaurantStatus.ACTIVE && newStatus != RestaurantStatus.CLOSED) {
                 throw new RuntimeException("Owners can only set ACTIVE or CLOSED status");
            }
            // Can only open if APPROVED or previously ACTIVE/CLOSED
            if (restaurant.getStatus() == RestaurantStatus.DRAFT || restaurant.getStatus() == RestaurantStatus.PENDING_REVIEW || restaurant.getStatus() == RestaurantStatus.REJECTED || restaurant.getStatus() == RestaurantStatus.SUSPENDED) {
                 throw new RuntimeException("Restaurant is not in a valid state to open");
            }
        }

        restaurant.setStatus(newStatus);
        return mapToResponse(restaurantRepository.save(restaurant));
    }

    public RestaurantResponse getMyRestaurant(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Restaurant> restaurants = restaurantRepository.findByOwnerId(owner.getId());
        if (restaurants.isEmpty()) {
            return null; // Or throw custom exception
        }
        // Assuming single restaurant for now, or fetch latest
        return mapToResponse(restaurants.get(0));
    }

    private Restaurant getRestaurantIfOwner(String restaurantId, String ownerEmail) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        if (!restaurant.getOwner().getEmail().equals(ownerEmail)) {
            throw new RuntimeException("Access denied");
        }
        return restaurant;
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .phone(restaurant.getPhone())
                .email(restaurant.getEmail())
                .cuisineTypes(restaurant.getCuisineTypes())
                .address(com.fooddelivery.dto.request.RestaurantAddressDto.builder()
                        .addressLine1(restaurant.getAddress().getAddressLine1())
                        .city(restaurant.getAddress().getCity())
                        .state(restaurant.getAddress().getState())
                        .pincode(restaurant.getAddress().getPincode())
                        .latitude(restaurant.getAddress().getLatitude())
                        .longitude(restaurant.getAddress().getLongitude())
                        .build())
                .openingTime(restaurant.getOpeningTime())
                .closingTime(restaurant.getClosingTime())
                .status(restaurant.getStatus())
                .ownerId(restaurant.getOwner().getId())
                .ownerName(restaurant.getOwner().getName())
                .gstNumber(restaurant.getGstNumber())
                .createdAt(restaurant.getCreatedAt() != null ? restaurant.getCreatedAt().toString() : null)
                .build();
    }
}
