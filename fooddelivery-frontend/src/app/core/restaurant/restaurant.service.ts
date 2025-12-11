import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Restaurant, RestaurantRequest, DocumentUploadRequest, RestaurantStatusUpdateRequest } from './restaurant.types';

@Injectable({
    providedIn: 'root'
})
export class RestaurantService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/restaurants';

    createRestaurant(data: RestaurantRequest): Observable<Restaurant> {
        return this.http.post<any>(this.apiUrl, data).pipe(map(res => res.data));
    }

    uploadDocuments(restaurantId: string, data: DocumentUploadRequest): Observable<{ message: string; verificationStatus: string }> {
        return this.http.post<any>(`${this.apiUrl}/${restaurantId}/documents`, data).pipe(map(res => res.data));
    }

    submitForReview(restaurantId: string): Observable<Restaurant> {
        return this.http.patch<any>(`${this.apiUrl}/${restaurantId}/submit-for-review`, {}).pipe(map(res => res.data));
    }

    getMyRestaurants(): Observable<Restaurant> {
        // API returns a single restaurant object wrapped in ApiResponse
        return this.http.get<any>(`${this.apiUrl}/mine`).pipe(
            map(response => response.data)
        );
    }

    getRestaurant(id: string): Observable<Restaurant> {
        return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data));
    }

    updateStatus(id: string, data: RestaurantStatusUpdateRequest): Observable<Restaurant> {
        return this.http.patch<any>(`${this.apiUrl}/${id}/status`, data).pipe(map(res => res.data));
    }
}
