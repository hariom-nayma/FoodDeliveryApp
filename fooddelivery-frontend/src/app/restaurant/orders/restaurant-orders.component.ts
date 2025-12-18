import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../core/services/order.service';

@Component({
    selector: 'app-restaurant-orders',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="container">
      <div class="header">
        <h1>Incoming Orders</h1>
      </div>

      <div class="orders-list">
        @if (loading()) {
            <p>Loading orders...</p>
        } @else if (orders().length > 0) {
            <div class="card" *ngFor="let order of orders()">
                <div class="order-header">
                    <span class="order-id">#{{ order.id.slice(0, 8) }}</span>
                    <span class="order-time">{{ order.createdAt | date:'mediumTime' }}</span>
                    <span class="status-badge" [class]="'status-' + order.status">{{ order.status }}</span>
                </div>
                
                <div class="order-items">
                    <div *ngFor="let item of order.items" class="item-row">
                        <span>{{ item.quantity }}x {{ item.name }}</span>
                        <span>₹{{ item.totalPrice }}</span>
                    </div>
                </div>
                
                <div class="order-footer">
                    <div class="total">Total: ₹{{ order.totalAmount }}</div>
                    <div class="actions">
                        @if (order.status === 'PLACED') {
                            <button class="btn-primary" (click)="updateStatus(order.id, 'ACCEPTED')">Accept</button>
                        }
                        @if (order.status === 'ACCEPTED') {
                            <button class="btn-primary" (click)="updateStatus(order.id, 'COOKING')">Start Cooking</button>
                        }
                        @if (order.status === 'COOKING' || order.status === 'ASSIGNED_TO_RIDER') {
                            <button class="btn-success" (click)="updateStatus(order.id, 'READY_FOR_PICKUP')">Ready for Pickup</button>
                        }
                    </div>
                </div>
            </div>

            <!-- Pagination Controls -->
             <div class="pagination">
                <button (click)="prevPage()" [disabled]="page() === 0">Previous</button>
                <span>Page {{ page() + 1 }} of {{ totalPages() }}</span>
                <button (click)="nextPage()" [disabled]="page() >= totalPages() - 1">Next</button>
             </div>

        } @else {
            <div class="empty-state">
                <p>No active orders right now.</p>
            </div>
        }
      </div>
    </div>
  `,
    styles: [`
    .container { max-width: 800px; margin: 2rem auto; }
    .card { background: white; border: 1px solid #eee; border-radius: 8px; padding: 1.5rem; margin-bottom: 1rem; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }
    .order-header { display: flex; justify-content: space-between; margin-bottom: 1rem; padding-bottom: 0.5rem; border-bottom: 1px solid #eee; }
    .order-id { font-weight: bold; }
    .status-badge { padding: 4px 8px; border-radius: 4px; font-size: 0.85rem; font-weight: bold; }
    .status-PLACED { background: #e3f2fd; color: #0d47a1; }
    .status-ACCEPTED { background: #FFF3E0; color: #E65100; }
    .status-COOKING { background: #fff3cd; color: #856404; }
    .status-READY_FOR_PICKUP { background: #d4edda; color: #155724; }
    
    .item-row { display: flex; justify-content: space-between; margin-bottom: 0.5rem; }
    .order-footer { margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
    .total { font-weight: bold; font-size: 1.1rem; }
    
    .actions { display: flex; gap: 10px; }
    button { padding: 8px 16px; border: none; border-radius: 4px; cursor: pointer; color: white; font-weight: 500; }
    .btn-primary { background: #e23744; }
    .btn-success { background: #28a745; }
    .pagination { display: flex; justify-content: center; gap: 1rem; align-items: center; margin-top: 1rem; }
    .pagination button { background: #f0f0f0; color: #333; }
    .pagination button:disabled { opacity: 0.5; cursor: not-allowed; }
  `]
})
export class RestaurantOrdersComponent implements OnInit {
    orderService = inject(OrderService);
    route = inject(ActivatedRoute); // To get Parent ID

    orders = signal<any[]>([]);
    loading = signal(true);
    restaurantId = '';
    page = signal(0);
    size = signal(10);
    totalElements = signal(0);
    totalPages = signal(0);

    ngOnInit() {
        // Access parent route params for restaurantId
        this.route.parent?.paramMap.subscribe(params => {
            const id = params.get('id');
            if (id) {
                this.restaurantId = id;
                this.loadOrders();
                // Optional: Poll every 30s
            }
        });
    }

    loadOrders() {
        this.loading.set(true);
        this.orderService.getRestaurantOrders(this.restaurantId, this.page(), this.size()).subscribe({
            next: (data: any) => {
                this.orders.set(data.content);
                this.totalElements.set(data.totalElements);
                this.totalPages.set(data.totalPages);
                this.loading.set(false);
            },
            error: () => this.loading.set(false)
        });
    }

    nextPage() {
        if (this.page() < this.totalPages() - 1) {
            this.page.update(p => p + 1);
            this.loadOrders();
        }
    }

    prevPage() {
        if (this.page() > 0) {
            this.page.update(p => p - 1);
            this.loadOrders();
        }
    }

    updateStatus(orderId: string, status: string) {
        if (!confirm(`Update status to ${status}?`)) return;

        this.orderService.updateOrderStatus(orderId, status).subscribe({
            next: () => {
                this.loadOrders(); // Refresh
            },
            error: (err) => alert('Failed to update status')
        });
    }
}
