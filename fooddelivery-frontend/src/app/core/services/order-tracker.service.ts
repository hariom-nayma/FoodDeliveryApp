import { Injectable, inject, signal, computed, effect } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap, retry, shareReplay, tap } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';

@Injectable({
    providedIn: 'root'
})
export class OrderTrackerService {
    private http = inject(HttpClient);
    private apiUrl = '/api/v1/orders/active';

    // Manual refresh trigger
    private refreshTrigger = signal(0);

    // Poll every 15 seconds
    activeOrdersResource = toSignal(
        timer(0, 15000).pipe(
            switchMap(() => this.http.get<any>(this.apiUrl)),
            retry(3), // Retry on failure
            tap(res => {
                // Optional: Check if status changed and notify
            })
        ),
        { initialValue: null }
    );

    activeOrders = computed(() => {
        const res = this.activeOrdersResource();
        return res?.data || [];
    });

    // Derived state: most relevant order (e.g., most recent)
    currentOrder = computed(() => {
        const orders = this.activeOrders();
        return orders.length > 0 ? orders[0] : null;
    });

    constructor() { }

    refresh() {
        // Logic to force refresh if using manual trigger, 
        // but timer handles polling.
    }
}
