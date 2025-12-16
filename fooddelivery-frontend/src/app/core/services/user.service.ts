import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Address {
    addressId?: string;
    line1: string;
    line2?: string;
    city: string;
    state: string;
    postalCode: string;
    latitude: number;
    longitude: number;
    label?: string;
    isDefault?: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class UserService {
    private apiUrl = `${environment.apiUrl}/addresses`;

    constructor(private http: HttpClient) { }

    getAddresses(): Observable<any> { // Wrapped in ApiResponse usually
        return this.http.get(this.apiUrl);
    }

    addAddress(address: Address): Observable<any> {
        return this.http.post(this.apiUrl, address);
    }

    updateAddress(id: string, address: Address): Observable<any> {
        return this.http.put(`${this.apiUrl}/${id}`, address);
    }

    deleteAddress(id: string): Observable<any> {
        return this.http.delete(`${this.apiUrl}/${id}`);
    }

    // Subscription
    initiateSubscription(): Observable<any> {
        return this.http.post(`${environment.apiUrl}/subscription/initiate`, {});
    }

    verifySubscription(payload: any): Observable<any> {
        return this.http.post(`${environment.apiUrl}/subscription/verify`, payload);
    }

    getUserProfile(): Observable<any> {
        return this.http.get(`${environment.apiUrl}/users/me`);
    }
}
