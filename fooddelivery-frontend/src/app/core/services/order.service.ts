import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
    providedIn: 'root'
})
export class OrderService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/orders';

    createOrder(cartId: string, addressId: string, paymentMethod: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/create`, { cartId, deliveryAddressId: addressId, paymentMethod }).pipe(map(res => res.data));
    }

    confirmPayment(orderId: string, paymentId: string, signature: string): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${orderId}/confirm-payment`, {
            paymentId,
            paymentGatewayOrderId: paymentId, // Optional, can confirm usage
            signature
        }).pipe(map(res => res.data));
    }

    getMyOrders(page: number = 0, size: number = 10): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/my?page=${page}&size=${size}`).pipe(map(res => res.data));
    }

    // For Restaurant
    getRestaurantOrders(restaurantId: string, page: number = 0, size: number = 10): Observable<any> {
        return this.http.get<any>(`/api/v1/restaurants/${restaurantId}/orders?page=${page}&size=${size}`).pipe(map(res => res.data));
    }

    updateOrderStatus(orderId: string, status: string): Observable<any> {
        // Map status to endpoint
        let endpoint = '';
        if (status === 'ACCEPTED') endpoint = 'accept';
        if (status === 'COOKING') endpoint = 'start-cooking'; // Backend: start-cooking
        if (status === 'READY_FOR_PICKUP') endpoint = 'ready';

        return this.http.patch<any>(`${this.apiUrl}/${orderId}/${endpoint}`, {}).pipe(map(res => res.data));
    }
}
