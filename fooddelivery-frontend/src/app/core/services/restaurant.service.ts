import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Restaurant, MenuItem, Category, RestaurantRequest, DocumentUploadRequest } from '../restaurant/restaurant.types';

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

    // Owner Methods
    createRestaurant(data: RestaurantRequest): Observable<Restaurant> {
        return this.http.post<ApiResponse<Restaurant>>(this.apiUrl, data).pipe(map(res => res.data));
    }

    getMyRestaurants(): Observable<Restaurant> {
        return this.http.get<ApiResponse<Restaurant>>(`${this.apiUrl}/mine`).pipe(
            map(response => response.data)
        );
    }

    updateRestaurant(id: string, data: any): Observable<Restaurant> {
        return this.http.put<ApiResponse<Restaurant>>(`${this.apiUrl}/${id}`, data).pipe(map(res => res.data));
    }

    uploadDocuments(restaurantId: string, data: DocumentUploadRequest): Observable<any> {
        return this.http.post<ApiResponse<any>>(`${this.apiUrl}/${restaurantId}/documents`, data).pipe(map(res => res.data));
    }

    submitForReview(restaurantId: string): Observable<Restaurant> {
        return this.http.patch<ApiResponse<Restaurant>>(`${this.apiUrl}/${restaurantId}/submit-for-review`, {}).pipe(map(res => res.data));
    }

    updateStatus(id: string, data: any): Observable<Restaurant> {
        return this.http.patch<ApiResponse<Restaurant>>(`${this.apiUrl}/${id}/status`, data).pipe(map(res => res.data));
    }
}
