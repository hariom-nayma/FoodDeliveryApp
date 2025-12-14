import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Category {
    id: string;
    name: string;
    description: string;
    sortOrder: number;
    active: boolean;
}

export interface MenuItem {
    id: string;
    name: string;
    description: string;
    basePrice: number;
    foodType: string;
    available: boolean;
    imageUrl: string;
    categoryId: string;
    optionGroups: any[]; // Define properly if needed
}

@Injectable({
    providedIn: 'root'
})
export class MenuService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/restaurants`; // /api/v1/restaurants

    getCategories(restaurantId: string): Observable<any> {
        return this.http.get(`${this.apiUrl}/${restaurantId}/categories?includeInactive=true`);
    }

    createCategory(restaurantId: string, data: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/${restaurantId}/categories`, data);
    }

    getMenuItems(restaurantId: string): Observable<any> {
        return this.http.get(`${this.apiUrl}/${restaurantId}/menu-items`);
    }

    createMenuItem(restaurantId: string, itemData: any, imageFile?: File): Observable<any> {
        const formData = new FormData();
        const blob = new Blob([JSON.stringify(itemData)], { type: 'application/json' });
        formData.append('item', blob);

        if (imageFile) {
            formData.append('image', imageFile);
        }

        return this.http.post(`${this.apiUrl}/${restaurantId}/menu-items`, formData);
    }

    updateMenuItem(restaurantId: string, itemId: string, itemData: any, imageFile?: File): Observable<any> {
        const formData = new FormData();
        const blob = new Blob([JSON.stringify(itemData)], { type: 'application/json' });
        formData.append('item', blob);

        if (imageFile) {
            formData.append('image', imageFile);
        }

        return this.http.put(`${this.apiUrl}/${restaurantId}/menu-items/${itemId}`, formData);
    }
}
