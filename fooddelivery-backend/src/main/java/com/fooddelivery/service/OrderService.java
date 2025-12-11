package com.fooddelivery.service;

import com.fooddelivery.dto.request.AddToCartRequest;
import com.fooddelivery.dto.request.CalculatePriceRequest;
import com.fooddelivery.dto.request.CreateOrderRequest;
import com.fooddelivery.dto.request.CartOptionRequest;
import com.fooddelivery.dto.response.PricingResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.OrderItemRepository;
import com.fooddelivery.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Order createOrder(String userId, CreateOrderRequest request) {
        Cart cart = cartService.getCartEntity(userId);
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 1. Calculate Price
        List<AddToCartRequest> itemRequests = cart.getItems().stream().map(i -> {
            AddToCartRequest req = new AddToCartRequest();
            req.setItemId(i.getMenuItem().getId());
            req.setQuantity(i.getQuantity());
            // Map options
            List<CartOptionRequest> opts = i.getOptions().stream().map(o -> {
                CartOptionRequest optReq = new CartOptionRequest();
                optReq.setGroupId(o.getOptionGroupId());
                optReq.setOptionId(o.getOptionId());
                return optReq;
            }).collect(Collectors.toList());
            req.setOptions(opts);
            return req;
        }).collect(Collectors.toList());

        CalculatePriceRequest priceReq = new CalculatePriceRequest();
        priceReq.setRestaurantId(cart.getRestaurant().getId());
        priceReq.setItems(itemRequests);
        priceReq.setDeliveryAddressId(request.getDeliveryAddressId());
        priceReq.setOfferCode(request.getOfferCode());

        PricingResponse pricing = pricingService.calculatePrice(priceReq);

        // 2. Create Order
        Order order = Order.builder()
                .user(cart.getUser())
                .restaurant(cart.getRestaurant())
                .status("COD".equalsIgnoreCase(request.getPaymentMethod()) ? OrderStatus.PLACED : OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .orderType(OrderType.DELIVERY) // Assume delivery for now
                .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(pricing.getEtaMinutes()))
                .subtotalAmount(pricing.getSubtotal())
                .discountAmount(pricing.getDiscount())
                .taxAmount(pricing.getTax())
                .deliveryFee(pricing.getDeliveryFee())
                .totalAmount(pricing.getTotal())
                .offerAppliedCode(pricing.getOfferApplied())
                .deliveryAddressJson(request.getDeliveryAddressId())
                .build();

        Order savedOrder = orderRepository.save(order);

        // 3. Save Items
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            String optionsJson = "[]";
            try {
                optionsJson = objectMapper.writeValueAsString(cartItem.getOptions());
            } catch (Exception e) {
            }

            return OrderItem.builder()
                    .order(savedOrder)
                    .menuItemId(cartItem.getMenuItem().getId())
                    .name(cartItem.getMenuItem().getName())
                    .quantity(cartItem.getQuantity())
                    .basePrice(cartItem.getItemPrice())
                    .totalPrice(cartItem.getTotalPrice()) // Note: Pricing service might have different total logic if
                                                          // options changed price, but Cart should be sync. Use cart
                                                          // snapshot.
                    .optionsJson(optionsJson)
                    .build();
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        // 4. Clear Cart
        cartService.clearCart(userId);

        return savedOrder;
    }

    @Transactional
    public Order confirmPayment(String orderId, String paymentId) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Order not in pending payment state");
        }
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PLACED);
        // Save payment ID logic if field exists
        return orderRepository.save(order);
    }

    // Status Updates
    @Transactional
    public Order updateStatus(String orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        // Validations can go here (state machine)
        order.setStatus(status);
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(String orderId, String userId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(userId)) { // Admin might bypass
            throw new RuntimeException("Unauthorized");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CANCELLED);
            // Initiate refund if PAID
            return orderRepository.save(order);
        } else {
            throw new RuntimeException("Cannot cancel order in current status");
        }
    }

    public List<Order> getMyOrders(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}
