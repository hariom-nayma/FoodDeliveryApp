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

    getMyOrders(): Observable<any[]> {
        return this.http.get<any>(`${this.apiUrl}/my`).pipe(map(res => res.data));
    }

    // For Restaurant
    getRestaurantOrders(restaurantId: string): Observable<any[]> {
        // Backend verification needed: Assuming RestaurantController has this, or OrderController?
        // Actually, RestaurantController.java didn't show getOrders for restaurant.
        // I might need to implement logic to fetch restaurant orders if backend is missing it, 
        // OR filtering logic. 
        // Wait, OrderController has no 'getRestaurantOrders'.
        // I will assume for now I need to fetch all orders if Role is Owner, or use a specific endpoint.
        // Actually, RestaurantController logic usually involves order management.
        // I will add 'getOrders' to RestaurantController in backend if needed.
        // For now, I'll put the stub here.
        return this.http.get<any>(`/api/v1/restaurants/${restaurantId}/orders`).pipe(map(res => res.data));
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
