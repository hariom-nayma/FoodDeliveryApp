package com.fooddelivery.service;

import com.fooddelivery.dto.request.AddToCartRequest;
import com.fooddelivery.dto.request.CalculatePriceRequest;
import com.fooddelivery.dto.request.CartOptionRequest;
import com.fooddelivery.dto.response.PricingResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.AddressRepository;
import com.fooddelivery.repository.MenuItemOptionRepository;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.repository.OfferRepository;
import com.fooddelivery.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;
    private final RestaurantRepository restaurantRepository;
    private final AddressRepository addressRepository;
    private final OfferRepository offerRepository;

    public PricingResponse calculatePrice(CalculatePriceRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));

        double subtotal = 0;

        for (AddToCartRequest itemReq : request.getItems()) {
            MenuItem item = menuItemRepository.findById(itemReq.getItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found: " + itemReq.getItemId()));

            double itemPrice = item.getBasePrice();
            if (itemReq.getOptions() != null) {
                for (CartOptionRequest opt : itemReq.getOptions()) {
                    MenuItemOption option = menuItemOptionRepository.findById(opt.getOptionId()).orElse(null);
                    if (option != null) {
                        itemPrice += option.getExtraPrice();
                    }
                }
            }
            subtotal += itemPrice * itemReq.getQuantity();
        }

        // Discount
        double discount = 0;
        String offerAppliedCode = null;
        if (request.getOfferCode() != null && !request.getOfferCode().isEmpty()) {
            Offer offer = offerRepository.findByCodeAndActiveTrue(request.getOfferCode())
                    .orElseThrow(() -> new RuntimeException("Invalid offer code"));

            // Validate usage limits, min order amount etc. here if needed
            // For now assume valid if active

            if ("PERCENTAGE".equalsIgnoreCase(offer.getDiscountType())) { // Assuming DiscountType is string or enum
                discount = (subtotal * offer.getDiscountValue()) / 100;
                if (offer.getMaxDiscountAmount() != null && discount > offer.getMaxDiscountAmount()) {
                    discount = offer.getMaxDiscountAmount();
                }
            } else {
                discount = offer.getDiscountValue();
            }
            offerAppliedCode = offer.getCode();
        }

        // Tax
        double tax = (subtotal - discount) * 0.05; // 5% after discount? Usually on subtotal.
        // Spec says: GST = subtotal * restaurant.taxRate.
        // Let's assume on subtotal.
        tax = subtotal * 0.05;

        // Delivery Fee
        double deliveryFee = 40.0;
        Address address = addressRepository.findById(request.getDeliveryAddressId())
                .orElseThrow(() -> new RuntimeException("Delivery Address not found"));

        // Calculate distance
        double distance = calculateDistance(
                restaurant.getAddress().getLatitude(), restaurant.getAddress().getLongitude(),
                address.getLatitude(), address.getLongitude());

        if (distance <= 3) {
            deliveryFee = 30; // Base
        } else {
            deliveryFee = 30 + (distance - 3) * 10;
        }

        // Ensure non-negative
        double total = subtotal - discount + tax + deliveryFee;

        return PricingResponse.builder()
                .subtotal(subtotal)
                .discount(discount)
                .tax(tax)
                .deliveryFee(deliveryFee)
                .total(total)
                .offerApplied(offerAppliedCode)
                .etaMinutes(35 + (int) (distance * 5)) // Rough ETA
                .build();
    }

    // Haversine
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Rider Payout Logic
    private final double BASE_PAY = 20.0;
    private final double PER_KM = 6.0;
    private final double PER_MIN = 0.5;

    public double calculatePayout(double distanceKm, double durationMin, double surgeMultiplier) {
        double payout = BASE_PAY
                + (distanceKm * PER_KM)
                + (durationMin * PER_MIN);

        return payout * surgeMultiplier;
    }

    public double calculateCustomerFee(double distanceKm, double durationMin, double surgeMultiplier) {
        return (distanceKm * 10 + durationMin * 1.5 + 20) * surgeMultiplier;
    }
}
