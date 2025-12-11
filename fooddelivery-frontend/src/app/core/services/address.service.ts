import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface Address {
    id: string;
    street: string;
    city: string;
    state: string;
    zipCode: string; // Updated to match likely backend mapping or DTO
    country: string;
    // Add other fields as per AddressResponse
}

@Injectable({
    providedIn: 'root'
})
export class AddressService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/addresses';

    getMyAddresses(): Observable<Address[]> {
        return this.http.get<any>(this.apiUrl).pipe(map(res => res.data));
    }

    addAddress(address: any): Observable<Address> {
        return this.http.post<any>(this.apiUrl, address).pipe(map(res => res.data));
    }
}
