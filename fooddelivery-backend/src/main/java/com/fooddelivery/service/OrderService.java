package com.fooddelivery.service;

import com.fooddelivery.dto.request.AddToCartRequest;
import com.fooddelivery.dto.request.CalculatePriceRequest;
import com.fooddelivery.dto.request.CreateOrderRequest;
import com.fooddelivery.dto.request.CartOptionRequest;
import com.fooddelivery.dto.response.OrderTrackingResponse;
import com.fooddelivery.dto.response.PricingResponse;
import com.fooddelivery.entity.*;
import com.fooddelivery.repository.AddressRepository;
import com.fooddelivery.repository.OrderItemRepository;
import com.fooddelivery.repository.OrderRepository;
import com.fooddelivery.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AddressRepository addressRepository;
    private final CartService cartService;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepo;
    private final DispatchService dispatchService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final WhatsAppService whatsAppService;
    private final com.corundumstudio.socketio.SocketIOServer socketIOServer;

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

        // Handle Null Restaurant in Cart (Data Recovery)
        Restaurant restaurant = cart.getRestaurant();
        if (restaurant == null) {
            // Assume all items are from same restaurant (enforced by add to cart)
            restaurant = cart.getItems().get(0).getMenuItem().getRestaurant();
            // Self-heal
            cart.setRestaurant(restaurant);
            cartService.saveCart(cart);
        }

        CalculatePriceRequest priceReq = new CalculatePriceRequest();
        priceReq.setRestaurantId(restaurant.getId());
        priceReq.setItems(itemRequests);
        priceReq.setDeliveryAddressId(request.getDeliveryAddressId());
        priceReq.setOfferCode(request.getOfferCode());
        priceReq.setUserId(userId);

        PricingResponse pricing = pricingService.calculatePrice(priceReq);

        // Fetch Address Snapshot
        Address address = addressRepository.findById(request.getDeliveryAddressId())
                .orElseThrow(() -> new RuntimeException("Address not found"));
        String addressJson = request.getDeliveryAddressId(); // Fallback
        try {
            addressJson = objectMapper.writeValueAsString(address);
        } catch (Exception e) {
        }

        // 2. Create Order
        Order order = Order.builder()
                .user(cart.getUser())
                .restaurant(restaurant)
                .status("COD".equalsIgnoreCase(request.getPaymentMethod()) ? OrderStatus.PLACED
                        : OrderStatus.PENDING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .orderType(OrderType.DELIVERY) // Assume delivery for now
                .estimatedDeliveryTime(LocalDateTime.now().plusMinutes(pricing.getEtaMinutes()))
                .subtotalAmount(pricing.getSubtotal())
                .discountAmount(pricing.getDiscount())
                .taxAmount(pricing.getTax())
                .deliveryFee(pricing.getDeliveryFee())
                .totalAmount(pricing.getTotal())
                .offerAppliedCode(pricing.getOfferApplied())
                .deliveryAddressJson(addressJson) // Storing full snapshot
                .build();

        // RAZORPAY INTEGRATION
        if ("ONLINE".equalsIgnoreCase(request.getPaymentMethod())) {
            String rzpOrderId = paymentService.createOrder(order.getTotalAmount(),
                    "order_" + System.currentTimeMillis());
            order.setRazorpayOrderId(rzpOrderId);
        }

        Order saved = orderRepository.save(order);

        // 3. Save Items
        List<OrderItem> orderItems = cart.getItems().stream().map(cartItem -> {
            String optionsJson = "[]";
            try {
                optionsJson = objectMapper.writeValueAsString(cartItem.getOptions());
            } catch (Exception e) {
            }

            return OrderItem.builder()
                    .order(saved)
                    .menuItemId(cartItem.getMenuItem().getId())
                    .name(cartItem.getMenuItem().getName())
                    .quantity(cartItem.getQuantity())
                    .basePrice(cartItem.getItemPrice())
                    .totalPrice(cartItem.getTotalPrice())
                    .optionsJson(optionsJson)
                    .build();
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);
        
        // PREPARE PAYLOAD BEFORE CLEARING CART
        java.util.List<java.util.Map<String, Object>> itemsPayload = cart.getItems().stream().map(i -> java.util.Map.of(
                "name", i.getMenuItem().getName(),
                "quantity", i.getQuantity(),
                "options", i.getOptions().stream().map(CartItemOption::getOptionName).collect(Collectors.toList())
        )).collect(Collectors.toList());

        // 4. Clear Cart
        cartService.clearCart(userId);

        // Notify WhatsApp
        whatsAppService.sendOrderPlacedNotification(saved);

        // Notify Restaurant Owner (Socket.IO)
        if (socketIOServer.getRoomOperations("restaurant_" + restaurant.getId()) != null) {
            String addressLabel = "Delivery Location";
            try {
                if (saved.getDeliveryAddressJson() != null && saved.getDeliveryAddressJson().startsWith("{")) {
                     com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(saved.getDeliveryAddressJson());
                     if (node.has("label")) addressLabel = node.get("label").asText();
                }
            } catch (Exception e) {}

            // Build payload
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "id", saved.getId(),
                    "orderId", saved.getId(), 
                    "customerName", saved.getUser().getName(),
                    "deliveryAddress", addressLabel,
                    "totalAmount", saved.getTotalAmount(),
                    "items", itemsPayload,
                    "createdAt", saved.getCreatedAt().toString());
            socketIOServer.getRoomOperations("restaurant_" + restaurant.getId()).sendEvent("new_order", payload);
            log.info("Sent new_order event to restaurant_{}", restaurant.getId());
        }

        return saved;
    }

    @Transactional
    public Order confirmPayment(String orderId, String paymentId, String signature) {
        Order order = getOrder(orderId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Order not in pending payment state");
        }

        // Verify Signature
        boolean isValid = paymentService.verifyPayment(order.getRazorpayOrderId(), paymentId, signature);
        if (!isValid) {
            throw new RuntimeException("Payment Verification Failed");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.PLACED);
        order.setPaymentId(paymentId);

        return orderRepository.save(order);
    }

    // Status Updates
    @Transactional
    public Order updateStatus(String orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        // Validations can go here (state machine)

        if (order.getStatus().equals(status)) {
            throw new RuntimeException("Order status is already " + status);
        }

        if (status == OrderStatus.READY_FOR_PICKUP && (order.getStatus().equals(OrderStatus.PICKED_UP)
                || order.getStatus().equals(OrderStatus.DELIVERED))) {
            throw new RuntimeException("Order status is not valid for " + status);
        }

        // Handle COD Payment on Delivery
        if (status == OrderStatus.DELIVERED) {
            log.info("Processing DELIVERED status for Order: {}", orderId);
            log.info("Payment Method: '{}'", order.getPaymentMethod());
            log.info("Delivery Partner: {}",
                    order.getDeliveryPartner() != null ? order.getDeliveryPartner().getId() : "NULL");

            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus(PaymentStatus.PAID);

                if (order.getDeliveryPartner() != null) {
                    log.info("Adding COD Collection Entry to Ledger");
                    walletService.addEntry(
                            order.getDeliveryPartner().getUserId(),
                            -order.getTotalAmount(),
                            RiderLedger.LedgerType.COLLECTION,
                            order.getId(),
                            "Cash Collected for Order #" + order.getId());
                } else {
                    log.warn("Delivery Partner is NULL for COD Order {}", orderId);
                }
            }
        }

        order.setStatus(status);
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());

            // 2. Log Earning (Credit)
            if (order.getDeliveryPartner() != null) {
                // Use the locked-in earning from assignment
                double earning = order.getRiderEarning() != null ? order.getRiderEarning() : 0.0;

                if (earning == 0.0) {
                    // Fallback if missing (shouldn't happen for new orders)
                    double dist = 5.0;
                    earning = pricingService.calculatePayout(dist, 30, 1.0);
                    log.warn("RiderEarning missing for order {}. Using fallback calculation: {}", orderId, earning);
                }

                log.info("Adding Earning Entry to Ledger: {}", earning);

                walletService.addEntry(
                        order.getDeliveryPartner().getUserId(),
                        earning,
                        RiderLedger.LedgerType.EARNING,
                        order.getId(),
                        "Ride Earnings for Order #" + order.getId());
            } else {
                log.warn("Delivery Partner is NULL for Earnings Order {}", orderId);
            }

            // Unlock Rider
            if (order.getDeliveryPartner() != null) {
                log.info("Unlocking Rider {} for Order {}", order.getDeliveryPartner().getId(), orderId);
                log.info("Delivery Partner User ID : {}", order.getDeliveryPartner().getUserId());
                dispatchService.releaseRiderLock(order.getDeliveryPartner().getId());
            }
        }

        // Trigger Dispatch if Cooking
        if (status == OrderStatus.COOKING) {
            dispatchService.dispatchOrder(orderId);
        }

        Order savedOrder = orderRepository.save(order);

        // Notify WhatsApp
        if (savedOrder.getStatus() == OrderStatus.PLACED) {
            whatsAppService.sendOrderPlacedNotification(savedOrder);
        } else if (savedOrder.getStatus() == OrderStatus.DELIVERED) {
            whatsAppService.sendOrderDeliveredNotification(savedOrder.getUser().getPhone(), savedOrder.getId());
        }

        return savedOrder;
    }

    @Transactional
    public Order cancelOrder(String orderId, String userId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(userId)) { // Admin might bypass
            throw new RuntimeException("Unauthorized");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT || order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CANCELLED);

            // Unlock Rider if assigned (Rare for Placed/Pending, but good safety)
            if (order.getDeliveryPartner() != null) {
                dispatchService.releaseRiderLock(order.getDeliveryPartner().getUserId());
            }

            // Initiate refund if PAID
            return orderRepository.save(order);
        } else {
            throw new RuntimeException("Cannot cancel order in current status");
        }
    }

    public com.fooddelivery.dto.response.PagedResponse<Order> getMyOrders(String userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId,
                pageable);

        return new com.fooddelivery.dto.response.PagedResponse<>(
                orderPage.getContent(),
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages());
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<OrderTrackingResponse> getActiveOrders(String userId) {
        List<Order> orders = orderRepository.findByUserIdAndStatusNotInOrderByCreatedAtDesc(userId, List.of(
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.REJECTED));

        return orders.stream()
                .map(this::buildTrackingResponse)
                .collect(Collectors.toList());
    }

    public OrderTrackingResponse getTrackingDetails(String orderId) {
        return buildTrackingResponse(getOrder(orderId));
    }

    private OrderTrackingResponse buildTrackingResponse(Order order) {
        // 1. User Location
        OrderTrackingResponse.Location userLoc = null;
        String addressRef = order.getDeliveryAddressJson();

        if (addressRef != null) {
            // A. Try parsing as JSON Snapshot
            try {
                if (addressRef.trim().startsWith("{")) {
                    com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(addressRef);
                    if (node.has("latitude") && node.has("longitude")) {
                        userLoc = OrderTrackingResponse.Location.builder()
                                .latitude(node.get("latitude").asDouble())
                                .longitude(node.get("longitude").asDouble())
                                .addressLabel(node.has("label") ? node.get("label").asText() : "Delivery Location")
                                .build();
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors, proceed to ID lookup
            }

            // B. Fallback: Treat as Address ID if snapshot failed
            if (userLoc == null) {
                try {
                    // Try to clean potential quotes if stored as JSON string "ID"
                    String addressId = addressRef.replace("\"", "").trim();
                    Optional<Address> addrOpt = addressRepository.findById(addressId);
                    if (addrOpt.isPresent()) {
                        Address addr = addrOpt.get();
                        userLoc = OrderTrackingResponse.Location.builder()
                                .latitude(addr.getLatitude())
                                .longitude(addr.getLongitude())
                                .addressLabel(addr.getLabel())
                                .build();
                    }
                } catch (Exception e) {
                }
            }
        }

        // 2. Restaurant Location
        OrderTrackingResponse.Location restLoc = null;
        if (order.getRestaurant() != null && order.getRestaurant().getAddress() != null) {
            RestaurantAddress addr = order.getRestaurant().getAddress();
            restLoc = OrderTrackingResponse.Location.builder()
                    .latitude(addr.getLatitude())
                    .longitude(addr.getLongitude())
                    .addressLabel(order.getRestaurant().getName())
                    .build();
        }

        // 3. Rider Location
        OrderTrackingResponse.Location riderLoc = null;
        String riderName = null;
        String riderPhone = null;
        String riderVehicle = null;
        String riderVehicleType = null;

        if (order.getDeliveryPartner() != null) {
            DeliveryPartner dp = order.getDeliveryPartner();

            Optional<User> user = userRepo.findById(dp.getUserId());
            if (user.isPresent()) {
                riderName = user.get().getName();
                riderPhone = user.get().getPhone();
                riderVehicle = "N/A";
                riderVehicleType = dp.getVehicleType();
            }

            if (dp.getCurrentLatitude() != null && dp.getCurrentLongitude() != null) {
                riderLoc = OrderTrackingResponse.Location.builder()
                        .latitude(dp.getCurrentLatitude())
                        .longitude(dp.getCurrentLongitude())
                        .addressLabel("Rider")
                        .build();
            }
        }

        return OrderTrackingResponse.builder()
                .orderId(order.getId())
                .status(order.getStatus().toString())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .userLocation(userLoc)
                .restaurantLocation(restLoc)
                .riderLocation(riderLoc)
                .restaurantName(order.getRestaurant().getName())
                .riderName(riderName)
                .riderPhone(riderPhone)
                .riderVehicleNumber(riderVehicle)
                .riderVehicleType(riderVehicleType)
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "PENDING")
                .build();
    }
}
