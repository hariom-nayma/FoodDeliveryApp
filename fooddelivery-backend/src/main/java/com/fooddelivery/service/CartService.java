package com.fooddelivery.service;

import com.fooddelivery.dto.request.AddToCartRequest;
import com.fooddelivery.dto.request.CartOptionRequest;
import com.fooddelivery.dto.request.UpdateCartRequest;
import com.fooddelivery.dto.response.CartItemResponse;
import com.fooddelivery.dto.response.CartOptionResponse;
import com.fooddelivery.dto.response.CartResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;

    @Transactional
    public CartResponse getMyCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToResponse(cart);
    }

    public Cart getCartEntity(String userId) {
        return getOrCreateCart(userId);
    }

    @Transactional
    public CartResponse addToCart(String userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        MenuItem item = menuItemRepository.findById(request.getItemId())
                .orElseThrow(() -> new RuntimeException("Menu Item not found"));

        // 1. Check Restaurant Conflict
        if (cart.getRestaurant() != null && !cart.getRestaurant().getId().equals(request.getRestaurantId())) {
            // Auto-clear
            cartItemRepository.deleteAll(cart.getItems());
            cart.getItems().clear();
            cart.setRestaurant(item.getRestaurant());
            cartRepository.save(cart);
        } else if (cart.getRestaurant() == null) {
            cart.setRestaurant(item.getRestaurant());
            cartRepository.save(cart);
        }

        // 2. Check for matching item (Merge logic)
        Optional<CartItem> existingItem = findMatchingItem(cart, request.getItemId(), request.getOptions());

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            updateItemPrice(cartItem); // Recalculate just in case
            cartItemRepository.save(cartItem);
        } else {
            // Create New
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .menuItem(item)
                    .quantity(request.getQuantity())
                    .itemPrice(item.getBasePrice())
                    .build();
            
            // Add options
            if (request.getOptions() != null) {
                for (CartOptionRequest optReq : request.getOptions()) {
                    MenuItemOption option = menuItemOptionRepository.findById(optReq.getOptionId())
                            .orElseThrow(() -> new RuntimeException("Option not found: " + optReq.getOptionId()));
                    
                    CartItemOption cartOption = CartItemOption.builder()
                            .optionGroupId(optReq.getGroupId()) // Or option.getOptionGroup().getId()
                            .optionGroupName(option.getOptionGroup().getName())
                            .optionId(option.getId())
                            .optionName(option.getLabel())
                            .price(option.getExtraPrice())
                            .build();
                    newItem.addOption(cartOption);
                }
            }
            updateItemPrice(newItem);
            cart.getItems().add(newItem);
            cartItemRepository.save(newItem);
        }

        return mapToResponse(cartRepository.save(cart)); // Save cart to be sure properties update
    }

    @Transactional
    public CartResponse updateCartItem(String userId, UpdateCartRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(request.getCartItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cart Item not found"));

        if (request.getQuantity() != null) {
            if (request.getQuantity() <= 0) {
                // Remove logic if qty 0? Spec says "DELETE /cart/item" is separate. 
                // But typically qty 0 means remove. Let's assume validation prevents <=0 or we remove.
                // Spec says "Quantity <=0 BAD_REQUEST", so we throw.
                throw new RuntimeException("Quantity must be > 0");
            }
            item.setQuantity(request.getQuantity());
        }

        // Updating options is complex (re-creating list), spec allows it.
        // For now, let's assume update is mostly Qty. 
        // If options provided, we replace.
        if (request.getOptions() != null) {
             // Clear existing options
             item.getOptions().clear();
             // Add new
             for (CartOptionRequest optReq : request.getOptions()) {
                MenuItemOption option = menuItemOptionRepository.findById(optReq.getOptionId())
                        .orElseThrow(() -> new RuntimeException("Option not found"));
                
                CartItemOption cartOption = CartItemOption.builder()
                        .optionGroupId(optReq.getGroupId())
                        .optionGroupName(option.getOptionGroup().getName())
                        .optionId(option.getId())
                        .optionName(option.getLabel())
                        .price(option.getExtraPrice())
                        .build();
                item.addOption(cartOption);
            }
        }

        updateItemPrice(item);
        cartItemRepository.save(item);
        return mapToResponse(cartRepository.findById(cart.getId()).orElseThrow());
    }

    @Transactional
    public CartResponse removeCartItem(String userId, String cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);
        
        // If empty, clear restaurant?
        if (cart.getItems().isEmpty()) {
            cart.setRestaurant(null);
        }
        
        return mapToResponse(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(String userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setRestaurant(null);
        cartRepository.save(cart);
    }

    // Helpers

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId).orElseThrow();
                    return cartRepository.save(Cart.builder().user(user).build());
                });
    }

    private Optional<CartItem> findMatchingItem(Cart cart, String itemId, List<CartOptionRequest> requestedOptions) {
        // Simple logic: Check if itemId matches AND option IDs match exactly.
        // Requested Options: List of {groupId, optionId}
        // Existing Options: List of CartItemOption {optionId...}
        
        List<String> reqOptionIds = requestedOptions == null ? new ArrayList<>() : 
            requestedOptions.stream().map(CartOptionRequest::getOptionId).sorted().collect(Collectors.toList());

        return cart.getItems().stream()
                .filter(ci -> ci.getMenuItem().getId().equals(itemId))
                .filter(ci -> {
                    List<String> existingIds = ci.getOptions().stream()
                            .map(CartItemOption::getOptionId).sorted().collect(Collectors.toList());
                    return existingIds.equals(reqOptionIds);
                })
                .findFirst();
    }

    private void updateItemPrice(CartItem item) {
        double base = item.getMenuItem().getBasePrice(); // Or item.getItemPrice() (snapshot)
        // Let's use current menu price or snapshot? Usually snapshot.
        // But if I use base from entity, I should update snapshot.
        item.setItemPrice(item.getMenuItem().getBasePrice()); 
        
        double optionsTotal = item.getOptions().stream().mapToDouble(CartItemOption::getPrice).sum();
        item.setTotalPrice((base + optionsTotal) * item.getQuantity());
    }

    private CartResponse mapToResponse(Cart cart) {
        double subtotal = cart.getItems().stream().mapToDouble(CartItem::getTotalPrice).sum();
        double tax = subtotal * 0.05; // 5% example tax
        double deliveryFee = subtotal > 0 ? 40.0 : 0.0; // Flat 40 if not empty
        double total = subtotal + tax + deliveryFee;

        List<CartItemResponse> items = cart.getItems().stream().map(i -> 
            CartItemResponse.builder()
                    .cartItemId(i.getId())
                    .itemId(i.getMenuItem().getId())
                    .name(i.getMenuItem().getName())
                    .quantity(i.getQuantity())
                    .basePrice(i.getItemPrice())
                    .finalPrice(i.getTotalPrice())
                    .options(i.getOptions().stream().map(o -> 
                        CartOptionResponse.builder()
                                .groupName(o.getOptionGroupName())
                                .optionSelected(o.getOptionName())
                                .extraPrice(o.getPrice())
                                .build()
                    ).collect(Collectors.toList()))
                    .build()
        ).collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .restaurantId(cart.getRestaurant() != null ? cart.getRestaurant().getId() : null)
                .items(items)
                .subtotal(subtotal)
                .tax(tax)
                .deliveryFee(deliveryFee)
                .total(total)
                .build();
    }
}
