export enum RestaurantStatus {
    DRAFT = 'DRAFT',
    PENDING_REVIEW = 'PENDING_REVIEW',
    APPROVED = 'APPROVED',
    REJECTED = 'REJECTED',
    SUSPENDED = 'SUSPENDED',
    ACTIVE = 'ACTIVE',
    CLOSED = 'CLOSED'
}

export interface RestaurantAddress {
    addressLine1: string;
    city: string;
    state: string;
    pincode: string;
    latitude: number;
    longitude: number;
}

export interface Restaurant {
    id: string;
    name: string;
    description: string;
    phone: string;
    email: string;
    cuisineTypes: string[];
    address: RestaurantAddress;
    openingTime: string;
    closingTime: string;
    status: RestaurantStatus;
    ownerId: string;
    ownerName: string;
    gstNumber?: string;
    createdAt: string;
}

export interface RestaurantRequest {
    name: string;
    description: string;
    phone: string;
    email: string;
    cuisineTypes: string[];
    address: RestaurantAddress;
    openingTime: string;
    closingTime: string;
}

export interface DocumentUploadRequest {
    gstNumber: string;
    fssaiNumber: string;
    documents: { type: string; fileUrl: string }[];
}

export interface RestaurantStatusUpdateRequest {
    status: string;
    reason?: string;
}
