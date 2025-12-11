import { Component, Input, Output, EventEmitter, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MapComponent } from '../../shared/components/map/map.component';

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
                <span>Accepted by Restaurant</span>
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

    .details { background: #f8f9fa; padding: 15px; border-radius: 8px; }
    .eta h3 { margin: 0; font-size: 1rem; color: #666; }
    .eta p { font-size: 1.5rem; font-weight: bold; margin: 5px 0; }
    
    .rider-info { display: flex; gap: 10px; margin-top: 15px; align-items: center; border-top: 1px solid #eee; padding-top: 15px; }
    .rider-img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; background: #eee; }
    
    @keyframes slideUp {
        from { transform: translateY(100%); }
        to { transform: translateY(0); }
    }
  `]
})
export class OrderTrackingModalComponent implements OnInit {
    @Input() order: any;
    @Output() close = new EventEmitter<void>();

    @ViewChild(MapComponent) mapComponent!: MapComponent;

    mapMarkers: any[] = [];
    trackingDetails: any = null; // Full details from API

    constructor() { }

    ngOnInit() {
        if (this.order) {
            this.trackingDetails = this.order;
            this.prepareMapMarkers();
        }
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
        return uiOrder.indexOf(status);
    }
}
