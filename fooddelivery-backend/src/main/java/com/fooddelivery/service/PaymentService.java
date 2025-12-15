package com.fooddelivery.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    private RazorpayClient client;

    @PostConstruct
    public void init() throws RazorpayException {
        // Initialize Razorpay Client
        this.client = new RazorpayClient(keyId, keySecret);
    }

    /**
     * Create a Razorpay Order
     * 
     * @param amount  Amount in double (will be converted to paisa)
     * @param receipt Receipt ID (e.g., internal Order ID)
     * @return Razorpay Order ID
     */
    public String createOrder(double amount, String receipt) {
        try {
            JSONObject options = new JSONObject();
            // Amount in paisa (e.g. 500.00 -> 50000)
            options.put("amount", (int) (amount * 100));
            options.put("currency", "INR");
            options.put("receipt", receipt);
            options.put("payment_capture", 1); // Auto capture

            com.razorpay.Order order = client.orders.create(options);
            return order.get("id");

        } catch (RazorpayException e) {
            throw new RuntimeException("Razorpay Order Creation Failed: " + e.getMessage(), e);
        }
    }

    /**
     * Verify Payment Signature
     * 
     * @param razorpayOrderId   The order ID returned by Razorpay
     * @param razorpayPaymentId The payment ID returned by Razorpay
     * @param signature         The signature returned by Razorpay
     */
    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", signature);

            return Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException e) {
            return false;
        }
    }
}
