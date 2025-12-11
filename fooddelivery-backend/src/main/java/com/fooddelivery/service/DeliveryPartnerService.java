package com.fooddelivery.service;

import com.fooddelivery.dto.request.DeliveryPartnerRequest;
import com.fooddelivery.dto.response.DeliveryPartnerResponse;
import com.fooddelivery.entity.DeliveryPartner;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeliveryPartnerService {

    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public DeliveryPartnerResponse submitApplication(String userId, DeliveryPartnerRequest request,
            MultipartFile license, MultipartFile aadhar, MultipartFile rc) {

        // check if already exists
        if (deliveryPartnerRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("Application already exists for this user");
        }

        String licenseUrl = cloudinaryService.uploadImage(license);
        String aadharUrl = cloudinaryService.uploadImage(aadhar);
        String rcUrl = cloudinaryService.uploadImage(rc);

        DeliveryPartner partner = DeliveryPartner.builder()
                .userId(userId)
                .vehicleType(request.getVehicleType())
                .status("PENDING")
                .drivingLicenseUrl(licenseUrl)
                .aadharCardUrl(aadharUrl)
                .vehicleRcUrl(rcUrl)
                .isOnline(false)
                .ratingAverage(0.0)
                .ratingCount(0)
                .totalDeliveriesCompleted(0)
                .build();

        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        return mapToResponse(saved);
    }

    public List<DeliveryPartnerResponse> getPendingApplications() {
        return deliveryPartnerRepository.findByStatus("PENDING").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryPartnerResponse approveApplication(String id) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));

        partner.setStatus("APPROVED");
        DeliveryPartner saved = deliveryPartnerRepository.save(partner);

        // Update User Role
        User user = userRepository.findById(partner.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(Role.ROLE_DELIVERY_PARTNER);
        userRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional
    public DeliveryPartnerResponse rejectApplication(String id) {
        DeliveryPartner partner = deliveryPartnerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));

        partner.setStatus("REJECTED");
        DeliveryPartner saved = deliveryPartnerRepository.save(partner);
        return mapToResponse(saved);
    }

    @Transactional
    public DeliveryPartnerResponse toggleOnlineStatus(String userId, boolean isOnline, Double lat, Double lng) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        partner.setOnline(isOnline);
        if (lat != null) partner.setCurrentLatitude(lat);
        if (lng != null) partner.setCurrentLongitude(lng);

        return mapToResponse(deliveryPartnerRepository.save(partner));
    }

    @Transactional
    public void updateLocation(String userId, Double lat, Double lng) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        
        partner.setCurrentLatitude(lat);
        partner.setCurrentLongitude(lng);
        deliveryPartnerRepository.save(partner);
    }
    
    public DeliveryPartnerResponse getProfile(String userId) {
        DeliveryPartner partner = deliveryPartnerRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
        return mapToResponse(partner);
    }

    private DeliveryPartnerResponse mapToResponse(DeliveryPartner partner) {
        return DeliveryPartnerResponse.builder()
                .id(partner.getId())
                .userId(partner.getUserId())
                .vehicleType(partner.getVehicleType())
                .status(partner.getStatus())
                .drivingLicenseUrl(partner.getDrivingLicenseUrl())
                .aadharCardUrl(partner.getAadharCardUrl())
                .vehicleRcUrl(partner.getVehicleRcUrl())
                .isOnline(partner.isOnline())
                .ratingAverage(partner.getRatingAverage())
                .totalDeliveriesCompleted(partner.getTotalDeliveriesCompleted())
                .createdAt(partner.getCreatedAt())
                .build();
    }
}
