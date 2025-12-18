import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { SocketService } from '../../core/services/socket.service';

@Component({
    selector: 'app-restaurant-orders',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="container">
      <div class="header">
        <h1>Orders Dashboard</h1>
        <button class="refresh-btn" (click)="loadOrders()">🔄 Refresh</button>
      </div>

      <div class="orders-list">
        @if (loading()) {
            <div class="loading-state">
                <div class="spinner"></div>
                <p>Fetching orders...</p>
            </div>
        } @else if (orders().length > 0) {
            <div class="order-grid">
                @for (order of orders(); track order.id) {
                    <div class="order-card" [class.border-left-new]="order.status === 'PLACED'">
                        <div class="card-header">
                            <div class="header-left">
                                <span class="order-id">#{{ order.id | slice:0:6 }}</span>
                                <span class="time-ago">{{ order.createdAt | date:'shortTime' }}</span>
                            </div>
                            <span class="status-badge" [class]="'status-' + order.status">{{ order.status }}</span>
                        </div>
                        
                        <div class="card-body">
                            <div class="customer-info" *ngIf="order.userLocation?.addressLabel">
                                📍 {{ order.userLocation.addressLabel }}
                            </div>

                            <div class="items-list">
                                <div *ngFor="let item of order.items" class="item-row">
                                    <div class="item-main">
                                        <span class="qty">{{ item.quantity }}x</span>
                                        <span class="name">{{ item.name }}</span>
                                    </div>
                                    <!-- Show Options -->
                                    <div class="item-options" *ngIf="getOptions(item).length > 0">
                                         <span *ngFor="let opt of getOptions(item)" class="opt-tag">{{ opt }}</span>
                                    </div>
                                    <!-- Fallback if options is string (json) -->
                                    <!-- <div *ngIf="isString(item.options)">{{item.options}}</div> -->
                                </div>
                            </div>
                        </div>
                        
                        <div class="card-footer">
                            <div class="amount">₹{{ order.totalAmount }}</div>
                            <div class="actions">
                                @if (order.status === 'PLACED') {
                                    <button class="btn-accept" (click)="updateStatus(order.id, 'ACCEPTED')">Accept</button>
                                } @else if (order.status === 'ACCEPTED') {
                                    <button class="btn-progress" (click)="updateStatus(order.id, 'COOKING')">Start Cooking</button>
                                } @else if (order.status === 'COOKING') {
                                    <button class="btn-ready" (click)="updateStatus(order.id, 'READY_FOR_PICKUP')">Ready</button>
                                }
                            </div>
                        </div>
                    </div>
                }
            </div>

            <!-- Pagination Controls -->
             <div class="pagination" *ngIf="totalPages() > 1">
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

      <!-- New Order Popup -->
      @if (incomingOrder()) {
        <div class="popup-overlay">
            <div class="popup-card">
                <div class="popup-header">
                    <div class="header-left">
                        <h2>🔥 New Order</h2>
                        <span class="popup-order-id">#{{ incomingOrder().id | slice:0:6 }}</span>
                    </div>
                    <div class="timer-container">
                        <span>{{ autoAcceptTimer() }}s</span>
                    </div>
                </div>
                
                <div class="popup-content">
                    <div class="popup-customer-info">
                        <strong>{{ incomingOrder().customerName }}</strong>
                        <span class="dot">•</span>
                        <span class="addr">{{ incomingOrder().deliveryAddress }}</span>
                    </div>

                    <span class="price-tag">₹{{ incomingOrder().totalAmount }}</span>
                    
                    <div class="items-scroll">
                        <div *ngFor="let item of incomingOrder().items" class="popup-item-wrapper">
                            <div class="popup-item-row">
                                <span class="item-qty">{{ item.quantity }}x</span>
                                <span class="item-name">{{ item.name }}</span>
                            </div>
                            <!-- Options Display -->
                            <div class="popup-options" *ngIf="item.options && item.options.length > 0">
                                <span *ngFor="let opt of item.options" class="popup-opt">{{ opt }}</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="popup-actions">
                    <button class="btn-close" (click)="closeIncomingPopup()">Ignore</button>
                    <button class="btn-accept-lg" (click)="acceptIncomingOrder()">
                        <span>Accept Order</span>
                        <span>→</span>
                    </button>
                </div>
                
                <!-- Progress Bar -->
                <div class="progress-bar-container">
                    <div class="progress-fill"></div>
                </div>
            </div>
        </div>
      }
    </div>
  `,
    styles: [`
    .container { max-width: 1000px; margin: 2rem auto; padding: 0 1rem; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
    h1 { font-size: 1.8rem; font-weight: 800; color: #111; margin: 0; }
    .refresh-btn { background: white; border: 1px solid #ddd; padding: 0.5rem 1rem; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
    .refresh-btn:hover { background: #f9f9f9; }

    .order-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.5rem; }
    
    .order-card { 
        background: white; 
        border: 1px solid #eee; 
        border-radius: 16px; 
        overflow: hidden; 
        transition: transform 0.2s, box-shadow 0.2s;
        display: flex; flex-direction: column;
    }
    .order-card:hover { transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.08); }
    .border-left-new { border-left: 5px solid #e23744; }

    .card-header { padding: 1rem; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; background: #fff; }
    .order-id { font-weight: 700; color: #333; }
    .time-ago { font-size: 0.8rem; color: #888; margin-left: 8px; }
    .status-badge { padding: 4px 10px; border-radius: 20px; font-size: 0.75rem; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; }
    
    .status-PLACED { background: #ffebee; color: #c62828; }
    .status-ACCEPTED { background: #fff3e0; color: #ef6c00; }
    .status-COOKING { background: #fff8e1; color: #f57f17; }
    .status-READY_FOR_PICKUP { background: #e8f5e9; color: #2e7d32; }
    .status-OUT_FOR_DELIVERY { background: #e3f2fd; color: #1565c0; }

    .card-body { padding: 1rem; flex: 1; }
    .customer-info { font-size: 0.85rem; color: #555; margin-bottom: 1rem; display: flex; gap: 6px; }
    
    .item-row { margin-bottom: 0.75rem; }
    .item-main { display: flex; gap: 8px; font-weight: 500; font-size: 0.95rem; color: #222; }
    .qty { color: #888; }
    .item-options { margin-left: 20px; font-size: 0.8rem; color: #666; margin-top: 4px; display: flex; flex-wrap: wrap; gap: 4px; }
    .opt-tag { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; }

    .card-footer { padding: 1rem; background: #fafafa; border-top: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
    .amount { font-weight: 800; font-size: 1.1rem; color: #111; }
    
    .actions button { padding: 8px 16px; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; transition: filter 0.2s; font-size: 0.85rem; }
    .btn-accept { background: #e23744; color: white; }
    .btn-progress { background: #ff9800; color: white; }
    .btn-ready { background: #4caf50; color: white; }
    
    .empty-state { text-align: center; padding: 4rem; color: #999; }
    .pagination { display: flex; justify-content: center; gap: 1rem; margin-top: 2rem; }

    /* POPUP Styles - Premium Overhaul */
    .popup-overlay { 
        position: fixed; inset: 0; 
        background: rgba(0, 0, 0, 0.65); 
        backdrop-filter: blur(8px); 
        z-index: 9999; 
        display: flex; justify-content: center; align-items: center; 
        animation: fadeIn 0.3s ease-out; 
    }
    
    .popup-card { 
        background: #fff; 
        width: 90%; max-width: 480px; 
        border-radius: 24px; 
        overflow: hidden;
        box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25); 
        animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
        position: relative;
    }

    /* Decorative top bar */
    .popup-card::before {
        content: '';
        position: absolute; top: 0; left: 0; right: 0; height: 6px;
        background: linear-gradient(90deg, #ff512f, #dd2476);
    }

    .popup-header { 
        padding: 2rem 2rem 1rem; 
        display: flex; justify-content: space-between; align-items: center; 
    }
    
    .popup-header h2 { 
        font-size: 1.5rem; 
        color: #1a1a1a; 
        margin: 0; 
        font-weight: 800; 
        display: flex; align-items: center; gap: 8px;
    }

    .timer-container {
        display: flex; align-items: center; gap: 8px;
        background: #fff0f0; color: #e23744;
        padding: 4px 12px; border-radius: 30px;
        font-weight: 800; font-size: 1rem;
        box-shadow: 0 2px 8px rgba(226, 55, 68, 0.15);
        animation: pulse-red 2s infinite;
    }

    .popup-order-id { font-size: 1rem; background: #f0f0f0; padding: 4px 8px; border-radius: 6px; color: #333; margin-top: 4px; display: inline-block; }
    .header-left { display: flex; flex-direction: column; align-items: flex-start; }

    .popup-customer-info { margin-bottom: 0.5rem; color: #555; font-size: 0.95rem; display: flex; align-items: center; justify-content: center; gap: 8px; }
    .dot { color: #ccc; }
    .addr { font-style: italic; }

    .popup-content { padding: 0 2rem 1.5rem; }
    
    .price-tag {
        font-size: 2.5rem; font-weight: 900; 
        color: #111; 
        letter-spacing: -1px;
        margin-bottom: 1.5rem;
        display: block;
    }

    .items-scroll {
        max-height: 180px; overflow-y: auto;
        background: #f8f9fa; border: 1px solid #eee;
        border-radius: 16px; padding: 1rem;
        margin-bottom: 2rem;
    }
    
    .popup-item-row {
        display: flex; gap: 12px; margin-bottom: 10px;
        font-size: 1rem; color: #444; border-bottom: 1px solid #f0f0f0; padding-bottom: 10px;
    }
    .popup-item-row:last-child { margin-bottom: 0; border-bottom: none; padding-bottom: 0; }
    
    .item-qty { font-weight: 700; color: #e23744; min-width: 24px; }
    .item-name { font-weight: 500; }

    .popup-actions { 
        padding: 1.5rem 2rem 2rem; 
        background: #fff;
        display: flex; gap: 1rem; 
    }
    
    .btn-close { 
        flex: 1;
        background: #f3f4f6; color: #4b5563; 
        padding: 1rem; border-radius: 14px; 
        border: none; font-weight: 600; font-size: 1rem;
        cursor: pointer; transition: all 0.2s;
    }
    .btn-close:hover { background: #e5e7eb; }

    .btn-accept-lg { 
        flex: 2;
        background: linear-gradient(135deg, #e23744 0%, #d32f2f 100%); 
        color: white; 
        padding: 1rem; border-radius: 14px; 
        border: none; font-weight: 700; font-size: 1rem;
        cursor: pointer; 
        box-shadow: 0 10px 20px rgba(226, 55, 68, 0.25);
        transition: transform 0.2s, box-shadow 0.2s;
        display: flex; justify-content: center; align-items: center; gap: 8px;
    }
    .btn-accept-lg:hover { transform: translateY(-2px); box-shadow: 0 15px 30px rgba(226, 55, 68, 0.35); }

    @keyframes fadeIn { from { opacity: 0; } }
    @keyframes slideUp { from { transform: translateY(40px); opacity: 0; } }
    @keyframes pulse-red { 0% { box-shadow: 0 0 0 0 rgba(226, 55, 68, 0.4); } 70% { box-shadow: 0 0 0 10px rgba(226, 55, 68, 0); } 100% { box-shadow: 0 0 0 0 rgba(226, 55, 68, 0); } }
    
    .progress-bar-container { height: 6px; background: #f0f0f0; width: 100%; position: absolute; bottom: 0; left: 0; }
    .progress-fill { height: 100%; background: linear-gradient(90deg, #e23744, #ff512f); width: 100%; animation: deplete 10s linear forwards; }
    @keyframes deplete { from { width: 100%; } to { width: 0%; } }

    .popup-options { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 4px; padding-left: 36px; }
    .popup-opt { font-size: 0.8rem; color: #666; background: #f5f5f5; padding: 2px 8px; border-radius: 4px; }
  `]
})
export class RestaurantOrdersComponent implements OnInit {
    orderService = inject(OrderService);
    route = inject(ActivatedRoute);

    orders = signal<any[]>([]);
    loading = signal(true);
    restaurantId = '';
    page = signal(0);
    size = signal(10);
    totalElements = signal(0);
    totalPages = signal(0);

    private socketService = inject(SocketService);

    // Popup State
    incomingOrder = signal<any>(null);
    autoAcceptTimer = signal(10);
    intervalId: any;

    ngOnInit() {
        this.route.parent?.paramMap.subscribe(params => {
            const id = params.get('id');
            if (id) {
                this.restaurantId = id;
                this.loadOrders();

                // Connect Socket
                this.socketService.joinRestaurantRoom(id);
                this.socketService.onNewOrder().subscribe((order) => {
                    this.handleIncomingOrder(order);
                });
            }
        });
    }

    handleIncomingOrder(order: any) {
        this.incomingOrder.set(order);
        this.autoAcceptTimer.set(10);

        // Start Timer
        this.intervalId = setInterval(() => {
            if (this.autoAcceptTimer() > 1) {
                this.autoAcceptTimer.update(v => v - 1);
            } else {
                // Auto Accept
                this.acceptIncomingOrder();
            }
        }, 1000);
    }

    acceptIncomingOrder() {
        this.stopTimer();
        const order = this.incomingOrder();
        if (order) {
            this.updateStatus(order.id, 'ACCEPTED');
            this.incomingOrder.set(null);
        }
    }

    closeIncomingPopup() {
        this.stopTimer();
        // Just dismiss, but allow manual accept from list? 
        // Or if closed, does it mean ignore? The prompt said "auto accept if pop up not closed". 
        // So closing manual stops auto accept.
        this.incomingOrder.set(null);
        // Refresh list to see the new order in PLACED state
        this.loadOrders();
    }

    stopTimer() {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = null;
        }
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
        // If coming from button, maybe confirm? For auto-accept no confirm.
        // Let's add simple check or just do it.

        this.orderService.updateOrderStatus(orderId, status).subscribe({
            next: () => {
                this.loadOrders(); // Refresh
            },
            error: (err) => console.error('Failed to update status')
        });
    }

    getOptions(item: any): string[] {
        if (item.options && Array.isArray(item.options) && item.options.length > 0) {
            return item.options;
        }
        if (item.optionsJson) {
            try {
                // It might be a JSON string of options, e.g. ["Cheese", "Spicy"]
                // Or a JSON string of objects.
                const parsed = JSON.parse(item.optionsJson);
                if (Array.isArray(parsed)) {
                    // Check if it's array of strings or objects
                    if (parsed.length > 0 && typeof parsed[0] === 'object') {
                        return parsed.map((p: any) => p.optionName || p.name);
                    }
                    return parsed;
                }
            } catch (e) {
                return [];
            }
        }
        return [];
    }
}
