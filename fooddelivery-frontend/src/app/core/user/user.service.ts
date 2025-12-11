import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../auth/auth.service';

export interface Address {
    id?: string;
    label: string;
    addressLine1: string;
    city: string;
    state: string;
    pincode: string;
    latitude: number;
    longitude: number;
    isDefault: boolean;
}

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/users/me';

    getUserProfile(): Observable<ApiResponse<User>> {
        return this.http.get<ApiResponse<User>>(this.apiUrl);
    }

    updateUserProfile(data: Partial<User>): Observable<ApiResponse<User>> {
        return this.http.put<ApiResponse<User>>(this.apiUrl, data);
    }

    getAddresses(): Observable<ApiResponse<Address[]>> {
        return this.http.get<ApiResponse<Address[]>>(`${this.apiUrl}/addresses`);
    }

    addAddress(address: Address): Observable<ApiResponse<Address>> {
        return this.http.post<ApiResponse<Address>>(`${this.apiUrl}/addresses`, address);
    }

    updateAddress(id: string, address: Address): Observable<ApiResponse<Address>> {
        return this.http.put<ApiResponse<Address>>(`${this.apiUrl}/addresses/${id}`, address);
    }

    deleteAddress(id: string): Observable<ApiResponse<string>> {
        return this.http.delete<ApiResponse<string>>(`${this.apiUrl}/addresses/${id}`);
    }
}
