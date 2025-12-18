import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CartOptionResponse {
    groupId: string;
    groupName: string;
    optionId: string;
    optionSelected: string;
    extraPrice: number;
}

export interface CartItemResponse {
    cartItemId: string;
    itemId: string;
    name: string;
    quantity: number;
    basePrice: number;
    finalPrice: number;
    options: CartOptionResponse[];
    imageUrl: string;
}

export interface CartResponse {
    cartId: string;
    restaurantId: string;
    items: CartItemResponse[];
    subtotal: number;
    tax: number;
    deliveryFee: number;
    total: number;
}

export interface AddToCartRequest {
    restaurantId: string;
    itemId: string;
    quantity: number;
    options?: { groupId: string, optionId: string }[];
}

export interface UpdateCartRequest {
    cartItemId: string;
    quantity?: number;
    options?: { groupId: string, optionId: string }[];
}

interface ApiResponse<T> {
    success: boolean;
    message: string;
    data: T;
}

@Injectable({
    providedIn: 'root'
})
export class CartService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/cart';

    // State
    cart = signal<CartResponse | null>(null);
    cartCount = computed(() => this.cart()?.items.reduce((acc, item) => acc + item.quantity, 0) || 0);

    constructor() {
        // Hydrate cart on load if needed, or call from AppComponent
        this.getCart().subscribe();
    }

    getCart(): Observable<CartResponse> {
        return this.http.get<ApiResponse<CartResponse>>(this.apiUrl).pipe(
            tap(res => {
                if (res.success) {
                    this.cart.set(res.data);
                }
            }),
            tap({
                error: () => this.cart.set(null) // Reset on error (e.g. 403)
            }),
            // Return raw data or unwrapped? 
            // Let's return unwrapped for consistency with other services if they do that, 
            // but here I use tap to update signal.
            // Map to data for subscriber convenience.
        ).pipe(res => res as any as Observable<CartResponse>); // Type assertion trick or map
    }

    // Better implementation of getCart with map
    fetchCart(): void {
        this.http.get<ApiResponse<CartResponse>>(this.apiUrl).subscribe(
            res => {
                if (res.success) this.cart.set(res.data);
            },
            err => this.cart.set(null)
        );
    }

    addToCart(request: AddToCartRequest): Observable<CartResponse> {
        return this.http.post<ApiResponse<CartResponse>>(`${this.apiUrl}/add`, request).pipe(
            tap(res => {
                if (res.success) this.cart.set(res.data);
            }),
            // map(res => res.data) // Map to data
        ) as any;
    }

    updateCartItem(request: UpdateCartRequest): Observable<CartResponse> {
        return this.http.put<ApiResponse<CartResponse>>(`${this.apiUrl}/update`, request).pipe(
            tap(res => {
                if (res.success) this.cart.set(res.data);
            })
        ) as any;
    }

    removeCartItem(cartItemId: string): Observable<CartResponse> {
        return this.http.delete<ApiResponse<CartResponse>>(`${this.apiUrl}/item/${cartItemId}`).pipe(
            tap(res => {
                if (res.success) this.cart.set(res.data);
            })
        ) as any;
    }

    clearCart(): Observable<void> {
        return this.http.delete<ApiResponse<void>>(`${this.apiUrl}/clear`).pipe(
            tap(res => {
                if (res.success) this.cart.set(null);
            })
        ) as any;
    }

    calculatePrice(payload: any): Observable<any> {
        return this.http.post(`${environment.apiUrl}/pricing/calculate`, payload);
    }
}
