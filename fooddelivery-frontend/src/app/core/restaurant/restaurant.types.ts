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
    imageUrl?: string;
    ratingAverage?: number;
    deliveryRadiusKm?: number;
    minOrderAmount?: number;
    cuisine?: string; // For backward compatibility or single string display
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

export interface Option {
    id: string;
    label: string;
    extraPrice: number;
}

export interface OptionGroup {
    id: string;
    name: string;
    multiSelect: boolean;
    required: boolean;
    options: Option[];
}

export interface MenuItem {
    id: string;
    name: string;
    description: string;
    basePrice: number;
    foodType: 'VEG' | 'NON_VEG' | 'VEGAN';
    imageUrl: string;
    categoryId: string;
    available?: boolean;
    optionGroups?: OptionGroup[];
}

export interface Category {
    id: string;
    name: string;
    restaurantId?: string;
}
