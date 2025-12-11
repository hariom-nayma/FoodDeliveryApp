import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-order-tracking-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="modal-backdrop" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
            <h2>Order Tracking</h2>
            <button class="close-btn" (click)="close.emit()">×</button>
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
            
             <div class="rider-info" *ngIf="order.deliveryPartner">
                <img [src]="order.deliveryPartner.drivingLicenseUrl || 'assets/rider-placeholder.png'" alt="Rider" class="rider-img"> <!-- Fallback image needed -->
                <div class="rider-text">
                     <strong>Rider on the way</strong>
                     <span>{{ order.deliveryPartner.vehicleType }}</span>
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
    }
    @media(min-width: 768px) {
        .modal-backdrop { align-items: center; }
        .modal-content { border-radius: 12px; }
    }
    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
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
export class OrderTrackingModalComponent {
  @Input() order: any;
  @Output() close = new EventEmitter<void>();

  // Helper to determine step state
  private statusOrder = ['PLACED', 'ACCEPTED', 'COOKING', 'READY_FOR_PICKUP', 'ASSIGNED_TO_RIDER', 'PICKED_UP', 'DELIVERED'];
  // Note: READY_FOR_PICKUP might happen before or after ASSIGNED_TO_RIDER depending on flow, but assuming linear progression for UI roughly.
  // Actually, ASSIGNED_TO_RIDER usually happens around COOKING/READY.
  // Let's simplify mapping:
  
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
       // Map others
       if (status === 'READY_FOR_PICKUP') return 2; // Show as Cooking complete
       return uiOrder.indexOf(status);
  }
}
