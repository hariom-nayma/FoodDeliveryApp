import { Component, Input, Output, EventEmitter, OnInit, AfterViewInit, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MapComponent } from '../../shared/components/map/map.component';
import { SocketService } from '../../core/services/socket.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-order-tracking-modal',
    standalone: true,
    imports: [CommonModule, MapComponent],
    template: `
    <div class="modal-backdrop" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
            <h2>Order Tracking</h2>
            <button class="close-btn" (click)="close.emit()">×</button>
        </div>
        
        <!-- Live Map -->
        <div class="map-wrapper" *ngIf="mapMarkers.length > 0">
             <app-map [markers]="mapMarkers" [showMarker]="false" style="height: 200px; display: block; border-radius: 12px; overflow: hidden; margin-bottom: 1rem;"></app-map>
        </div>
        
        <div class="status-timeline">
            <div class="step" [class.active]="isStepActive('PLACED')" [class.completed]="isStepCompleted('PLACED')">
                <div class="dot"></div>
                <span>Order Placed</span>
            </div>
            <div class="step" [class.active]="isStepActive('ACCEPTED')" [class.completed]="isStepCompleted('ACCEPTED')">
                <div class="dot"></div>
                <span>Accepted</span>
            </div>
             <!-- Escalation Warning -->
            <div class="step warning" *ngIf="order?.status === 'NO_RIDER_AVAILABLE' || showEscalationDialog" style="opacity:1; color:#ff9800;">
                 <div class="dot" style="background:#ff9800; animation: pulse 1s infinite"></div>
                 <span>Finding Premium Partner...</span>
            </div>
            
            <div class="step" [class.active]="isStepActive('COOKING')" [class.completed]="isStepCompleted('COOKING')">
                <div class="dot"></div>
                <span>Cooking</span>
            </div>
            <div class="step" [class.active]="isStepActive('ASSIGNED_TO_RIDER')" [class.completed]="isStepCompleted('ASSIGNED_TO_RIDER')">
                <div class="dot"></div>
                <span>Rider Assigned</span>
            </div>
             <div class="step" [class.active]="isStepActive('PICKED_UP')" [class.completed]="isStepCompleted('PICKED_UP')">
                <div class="dot"></div>
                <span>Out for Delivery</span>
            </div>
            <div class="step">
                <div class="dot"></div>
                <span>Delivered</span>
            </div>
        </div>

        <div class="details">
            <div class="eta">
                <h3>Estimated Arrival</h3>
                <p>{{ order.estimatedDeliveryTime | date:'shortTime' }}</p>
            </div>
            
             <div class="rider-info" *ngIf="trackingDetails?.riderName">
                <img [src]="'assets/rider-placeholder.png'" alt="Rider" class="rider-img"> 
                <div class="rider-text">
                     <strong>{{ trackingDetails.riderName }}</strong>
                     <span>{{ trackingDetails.riderVehicleType }} - {{ trackingDetails.riderVehicleNumber }}</span>
                </div>
                <div class="rider-contact" *ngIf="trackingDetails.riderPhone">
                    📞 {{ trackingDetails.riderPhone }}
                </div>
            </div>
        </div>
      </div>
      
       <!-- PREMIUM DIALOG OVERLAY -->
       <div class="premium-overlay" *ngIf="showEscalationDialog">
           <div class="premium-card">
               <div class="icon">✨</div>
               <h3>High Demand Area</h3>
               <p>We are escalating your request to our top-rated delivery partners. This ensures you get the best service, though it might take a moment longer.</p>
               <button (click)="showEscalationDialog = false">Track Request</button>
           </div>
       </div>
    </div>
  `,
    styles: [`
    .modal-backdrop {
        position: fixed; top: 0; left: 0; width: 100%; height: 100%;
        background: rgba(0,0,0,0.5); z-index: 10000;
        display: flex; justify-content: center; align-items: flex-end;
    }
    .modal-content {
        background: white; width: 100%; max-width: 500px;
        border-radius: 20px 20px 0 0; padding: 20px;
        animation: slideUp 0.3s ease-out;
        max-height: 90vh; /* Scrollable if needed */
        overflow-y: auto;
    }
    @media(min-width: 768px) {
        .modal-backdrop { align-items: center; }
        .modal-content { border-radius: 12px; }
    }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .close-btn { background: none; border: none; font-size: 2rem; cursor: pointer; }
    
    .status-timeline { margin-bottom: 2rem; padding-left: 10px; }
    .step { display: flex; align-items: center; gap: 15px; margin-bottom: 15px; position: relative; opacity: 0.5; }
    .step.completed, .step.active { opacity: 1; font-weight: bold; color: #333; }
    .step .dot { width: 12px; height: 12px; background: #ccc; border-radius: 50%; z-index: 2; }
    .step.completed .dot, .step.active .dot { background: #28a745; }
    
    .step:not(:last-child)::after {
        content: ''; position: absolute; left: 5px; top: 15px; bottom: -15px; width: 2px; background: #eee;
    }
    .step.completed:not(:last-child)::after { background: #28a745; }
    
    @keyframes pulse { 0% { transform: scale(1); } 50% { transform: scale(1.2); } 100% { transform: scale(1); } }

    .details { background: #f8f9fa; padding: 15px; border-radius: 8px; }
    .eta h3 { margin: 0; font-size: 1rem; color: #666; }
    .eta p { font-size: 1.5rem; font-weight: bold; margin: 5px 0; }
    
    .rider-info { display: flex; gap: 10px; margin-top: 15px; align-items: center; border-top: 1px solid #eee; padding-top: 15px; }
    .rider-img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; background: #eee; }
    
    /* Premium Dialog Styles */
    .premium-overlay {
        position: absolute; top:0; left:0; width:100%; height:100%;
        background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);
        display: flex; justify-content: center; align-items: center;
        border-radius: 12px; z-index: 10001; /* Above map */
    }
    .premium-card {
        background: linear-gradient(135deg, #fff 0%, #f8f9fa 100%);
        padding: 2rem; text-align: center; border-radius: 16px;
        width: 80%; max-width: 350px;
        box-shadow: 0 10px 30px rgba(0,0,0,0.2);
        animation: popIn 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    }
    .icon { font-size: 3rem; margin-bottom: 1rem; animation: float 3s ease-in-out infinite; }
    .premium-card h3 { margin: 0 0 0.5rem 0; color: #2c3e50; font-family: 'Outfit', sans-serif; }
    .premium-card p { color: #555; font-size: 0.95rem; line-height: 1.5; margin-bottom: 1.5rem; }
    .premium-card button {
        background: #000; color: #fff; border: none; padding: 12px 24px;
        border-radius: 50px; font-weight: bold; cursor: pointer;
        transition: transform 0.2s;
    }
    .premium-card button:hover { transform: scale(1.05); }
    
    @keyframes float { 0% { transform: translateY(0px); } 50% { transform: translateY(-10px); } 100% { transform: translateY(0px); } }
    @keyframes popIn { from { transform: scale(0.8); opacity: 0; } to { transform: scale(1); opacity: 1; } }
    
    @keyframes slideUp {
        from { transform: translateY(100%); }
        to { transform: translateY(0); }
    }
  `]
})
export class OrderTrackingModalComponent implements OnInit, OnDestroy {
    @Input() order: any;
    @Output() close = new EventEmitter<void>();

    @ViewChild(MapComponent) mapComponent!: MapComponent;

    mapMarkers: any[] = [];
    trackingDetails: any = null; // Full details from API
    
    showEscalationDialog = false;
    private sub: Subscription = new Subscription();

    constructor(private socketService: SocketService) { }

    ngOnInit() {
        if (this.order) {
            this.trackingDetails = this.order;
            this.prepareMapMarkers();
            
            // Initial check
            if (this.order.status === 'NO_RIDER_AVAILABLE') {
                this.showEscalationDialog = true;
            }
            
            // Listen for escalation
            this.sub.add(this.socketService.onOrderEscalated().subscribe(data => {
                if (data.orderId === this.order.id || data.orderId === this.order.orderId) {
                    this.showEscalationDialog = true;
                    this.order.status = 'NO_RIDER_AVAILABLE'; // Update local status
                }
            }));
            
            // Should verify if we need to join room manually here or if done globally
            // If order has user id, try joining
            // if (this.order.userId) this.socketService.joinUserRoom(this.order.userId);
        }
    }
    
    ngOnDestroy() {
        this.sub.unsubscribe();
    }

    ngAfterViewInit() {
        // Slight delay to ensure DOM render
        setTimeout(() => {
            if (this.mapComponent && this.mapMarkers.length > 0) {
                this.mapComponent.initializeMap();
            }
        }, 300);
    }

    prepareMapMarkers() {
        if (!this.trackingDetails) return;

        this.mapMarkers = [];
        const details = this.trackingDetails;

        // 1. Restaurant Location
        if (details.restaurantLocation) {
            this.mapMarkers.push({
                lat: details.restaurantLocation.latitude,
                lng: details.restaurantLocation.longitude,
                title: details.restaurantLocation.addressLabel || 'Restaurant',
                type: 'restaurant',
                icon: 'assets/restaurant-marker.png'
            });
        }

        // 2. User Location
        if (details.userLocation) {
            this.mapMarkers.push({
                lat: details.userLocation.latitude,
                lng: details.userLocation.longitude,
                title: details.userLocation.addressLabel || 'Delivery Location',
                type: 'user',
                icon: 'assets/deliverlocation-marker.png'
            });
        }


        // 3. Rider Location
        if (details.riderLocation) {
            this.mapMarkers.push({
                lat: details.riderLocation.latitude,
                lng: details.riderLocation.longitude,
                title: details.riderName + ' (Rider)',
                type: 'rider',
                icon: 'assets/deliveryman-marker.png'
            });
        }

        // Trigger Map Init
        setTimeout(() => {
            if (this.mapComponent) {
                this.mapComponent.markers = this.mapMarkers;
                this.mapComponent.initializeMap();
            }
        }, 100);
    }

    // Helper to determine step state
    private statusOrder = ['PLACED', 'ACCEPTED', 'COOKING', 'READY_FOR_PICKUP', 'ASSIGNED_TO_RIDER', 'PICKED_UP', 'DELIVERED'];

    get currentStepIndex() {
        // Map current status to index
        const status = this.order.status;
        if (this.showEscalationDialog || status === 'NO_RIDER_AVAILABLE') return 2; // Stuck at Accepted/Cooking
        
        if (status === 'ASSIGNED_TO_RIDER') return 3; // Treat as after COOKING
        if (status === 'READY_FOR_PICKUP') return 2; // Treat same as COOKING for simplicity or add step
        if (status === 'PICKED_UP') return 4;
        return this.statusOrder.indexOf(status);
    }

    isStepCompleted(stepName: string) {
        return this.getStatusIndex(this.order.status) > this.getStatusIndex(stepName);
    }

    isStepActive(stepName: string) {
        return this.order.status === stepName;
    }

    getStatusIndex(status: string) {
        // Custom order for UI visualization
        const uiOrder = ['PLACED', 'ACCEPTED', 'COOKING', 'ASSIGNED_TO_RIDER', 'PICKED_UP', 'DELIVERED'];
        if (status === 'READY_FOR_PICKUP') return 2; // Show as Cooking complete
        if (status === 'NO_RIDER_AVAILABLE') return 2; // Show near cooking
        return uiOrder.indexOf(status);
    }
}
