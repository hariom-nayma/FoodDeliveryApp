import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // For ngClass etc
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { RestaurantService } from '../../core/services/restaurant.service';
import { Restaurant, RestaurantStatus } from '../../core/restaurant/restaurant.types';
import { OrderService } from '../../core/services/order.service';

@Component({
    selector: 'app-owner-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './owner-dashboard.component.html',
    styles: [`
    .container { max-width: 1000px; margin: 2rem auto; padding: 1rem; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    .status-badge { padding: 0.5rem 1rem; border-radius: 999px; font-weight: bold; text-transform: uppercase; font-size: 0.875rem; }
    .status-DRAFT { background: #eee; color: #666; }
    .status-PENDING_REVIEW { background: #fff3cd; color: #856404; }
    .status-APPROVED { background: #d4edda; color: #155724; }
    .status-REJECTED { background: #f8d7da; color: #721c24; }
    .status-ACTIVE { background: #cce5ff; color: #004085; }
    
    .card { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 1rem; }
    .actions { display: flex; gap: 1rem; margin-top: 1rem; }
    button { padding: 0.75rem 1.5rem; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; }
    .btn-primary { background: var(--primary-color); color: white; }
    .btn-outline { background: transparent; border: 1px solid var(--primary-color); color: var(--primary-color); }
    .btn-danger { background: #dc3545; color: white; }
    .menu-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-top: 1rem; }
    .menu-card { background: #f8f9fa; padding: 1.5rem; border-radius: 8px; text-align: center; }
    .menu-card h4 { margin: 0 0 0.5rem; font-size: 1.1rem; }
    .menu-card p { margin: 0; font-size: 0.9rem; color: #666; }
  `]
})
export class OwnerDashboardComponent implements OnInit {
    private service = inject(RestaurantService);
    private orderService = inject(OrderService);
    private route = inject(ActivatedRoute);

    restaurant = signal<Restaurant | null>(null);
    loading = signal(true);
    error = signal('');
    orders = signal<any[]>([]);
    RestaurantStatus = RestaurantStatus;

    ngOnInit() {
        this.route.paramMap.subscribe(params => {
            const id = params.get('id');
            if (id) {
                this.loadRestaurant(id);
            } else {
                this.error.set('No restaurant ID provided');
                this.loading.set(false);
            }
        });
    }

    loadRestaurant(id: string) {
        this.service.getRestaurant(id).subscribe({
            next: (res) => {
                this.restaurant.set(res);
                this.loading.set(false);
                if (res.status === 'APPROVED' || res.status === 'ACTIVE') {
                    this.loadOrders(id);
                }
            },
            error: (err) => {
                this.error.set((err as any).error?.message || 'Failed to load restaurant');
                this.loading.set(false);
            }
        });
    }

    loadOrders(restaurantId: string) {
        this.orderService.getRestaurantOrders(restaurantId).subscribe({
            next: (orders) => this.orders.set(orders),
            error: (err) => console.error('Failed to load orders', err)
        });
    }

    submitForReview() {
        const r = this.restaurant();
        if (!r) return;

        if (confirm('Are you sure you want to submit for review? You cannot edit details while under review.')) {
            this.service.submitForReview(r.id).subscribe({
                next: (updated) => {
                    this.restaurant.set(updated);
                    alert('Submitted successfully!');
                },
                error: (err) => alert(err.error?.message || 'Submission failed')
            });
        }
    }
}
