package com.fooddelivery.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String WHATSAPP_API_URL = "http://localhost:3000/api/send-message";

    @Async
    public void sendOrderPlacedNotification(com.fooddelivery.entity.Order order) {
        String phone = order.getUser().getPhone();
        String userName = order.getUser().getName();
        String restaurantName = order.getRestaurant().getName();
        String restaurantImage = order.getRestaurant().getImageUrl();
        Double amount = order.getTotalAmount();
        String orderId = order.getId();

        StringBuilder msg = new StringBuilder();
        msg.append("👋 Hi ").append(userName).append(",\n\n");
        msg.append("🎉 Your order from *").append(restaurantName).append("* has been placed successfully!\n\n");
        msg.append("🧾 *Order ID:* ").append(orderId).append("\n");
        msg.append("💰 *Amount:* ₹").append(String.format("%.2f", amount)).append("\n\n");
        msg.append("We will notify you when your food is on the way. Sit tight! 🚀\n\n");

        // Simulating buttons with links as buttons are often restricted
        // msg.append("📍 *Track Order:*
        // payment.fooddelivery.com/track/").append(orderId).append("\n");
        // msg.append("❓ *Help:* payment.fooddelivery.com/support");

        sendMessage(phone, msg.toString(), restaurantImage);
    }

    @Async
    public void sendOrderDeliveredNotification(String customerPhone, String orderId) {
        String message = String.format(
                "🍽️ Order Delivered!\n\nOrder ID: %s has been delivered.\n\nEnjoy your meal! 😋\n\n⭐ *Rate Order* ",
                orderId);

        sendMessage(customerPhone, message, null);

        // Send Feedback Poll
        java.util.List<String> options = java.util.Arrays.asList(
                "⚡ Fast & Delicious",
                "🍔 Food was great (delivery ok)",
                "🕒 Delivery slow but food good",
                "😕 Needs improvement");
        sendPoll(customerPhone, "🚀 How was your overall experience?", options);
    }

    @Async
    public void sendPoll(String phone, String question, java.util.List<String> options) {
        try {
            if (phone == null || phone.isEmpty()) {
                log.warn("WHATSAPP: Skipping poll, no phone number provided.");
                return;
            }

            // For indian phone numbers
            if (phone.length() < 12) {
                phone = "91" + phone;
            }

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("phone", phone);

            java.util.Map<String, Object> poll = new java.util.HashMap<>();
            poll.put("question", question);
            poll.put("options", options);

            payload.put("poll", poll);

            restTemplate.postForEntity(WHATSAPP_API_URL, payload, String.class);
            log.info("WHATSAPP: Sent poll to {}", phone);
        } catch (Exception e) {
            log.error("WHATSAPP: Failed to send poll to {}. Service might be down. Error: {}", phone,
                    e.getMessage());
        }
    }

    @Async
    public void sendMessage(String phone, String message, String imageUrl) {
        try {
            if (phone == null || phone.isEmpty()) {
                log.warn("WHATSAPP: Skipping notification, no phone number provided.");
                return;
            }

            // For indian phone numbers
            if (phone.length() < 12) {
                phone = "91" + phone;
            }

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("phone", phone);
            payload.put("message", message);
            if (imageUrl != null && !imageUrl.isEmpty()) {
                payload.put("imageUrl", imageUrl);
            }

            restTemplate.postForEntity(WHATSAPP_API_URL, payload, String.class);
            log.info("WHATSAPP: Sent message to {}", phone);
        } catch (Exception e) {
            log.error("WHATSAPP: Failed to send message to {}. Service might be down. Error: {}", phone,
                    e.getMessage());
        }
    }

    // Overload for backward compatibility
    public void sendMessage(String phone, String message) {
        sendMessage(phone, message, null);
    }

}
