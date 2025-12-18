package com.fooddelivery.entity;

public enum OrderStatus {
    PENDING_PAYMENT,
    PLACED,
    ACCEPTED,
    COOKING,
    READY_FOR_PICKUP,
    SEARCHING_RIDER, // New
    OFFER_SENT, // New
    ASSIGNED_TO_RIDER, // Deprecated? Or used for legacy? Keeping it.
    RIDER_ACCEPTED, // New
    PICKED_UP,
    DELIVERED,
    NO_RIDER_AVAILABLE, // Escalation State
    CANCELLED,
    REJECTED
}
