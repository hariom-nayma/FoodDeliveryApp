package com.fooddelivery.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    ACCEPTED,
    COOKING,
    READY_FOR_PICKUP,
    ASSIGNED_TO_RIDER,
    PICKED_UP,
    DELIVERED,
    CANCELLED
}
