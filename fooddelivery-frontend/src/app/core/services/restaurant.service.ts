import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Restaurant {
    id: string;
    name: string;
    description: string;
    cuisine: string; // Add this to backend if missing or use description
    imageUrl: string; // logoUrl
    ratingAverage: number;
    deliveryRadiusKm: number;
    minOrderAmount: number;
    status: string;
}

export interface MenuItem {
    id: string;
    name: string;
    description: string;
    basePrice: number;
    foodType: 'VEG' | 'NON_VEG' | 'VEGAN';
    imageUrl: string;
    categoryId: string;
}

export interface Category {
    id: string;
    name: string;
}

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

@Injectable({
    providedIn: 'root'
})
export class RestaurantService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/restaurants';

    getRestaurants(city?: string): Observable<Restaurant[]> {
        let url = `${this.apiUrl}/search`;
        if (city) {
            url += `?city=${encodeURIComponent(city)}`;
        }
        return this.http.get<ApiResponse<Restaurant[]>>(url).pipe(
            map(res => res.data)
        );
    }

    getRestaurant(id: string): Observable<Restaurant> {
        return this.http.get<ApiResponse<Restaurant>>(`${this.apiUrl}/${id}`).pipe(
            map(res => res.data)
        );
    }

    getCategories(restaurantId: string): Observable<Category[]> {
        return this.http.get<ApiResponse<Category[]>>(`${this.apiUrl}/${restaurantId}/categories`).pipe(
            map(res => res.data)
        );
    }

    getMenuItems(restaurantId: string): Observable<MenuItem[]> {
        return this.http.get<ApiResponse<MenuItem[]>>(`${this.apiUrl}/${restaurantId}/menu-items`).pipe(
            map(res => res.data)
        );
    }
}
