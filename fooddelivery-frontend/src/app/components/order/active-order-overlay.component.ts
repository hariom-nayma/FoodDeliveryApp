import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderTrackerService } from '../../core/services/order-tracker.service';
import { OrderTrackingModalComponent } from './order-tracking-modal.component';

@Component({
    selector: 'app-active-order-overlay',
    standalone: true,
    imports: [CommonModule, OrderTrackingModalComponent],
    template: `
    @if (order()) {
      <div class="overlay-container" (click)="openModal()">
         <div class="content">
            <div class="icon">🛵</div>
            <div class="info">
               <span class="status">{{ order().status }}</span>
               <span class="restaurant">{{ order().restaurant?.name }}</span>
            </div>
            <div class="arrow">›</div>
         </div>
      </div>

      @if (showModal()) {
          <app-order-tracking-modal [order]="order()" (close)="closeModal()"></app-order-tracking-modal>
      }
    }
  `,
    styles: [`
    .overlay-container {
        position: fixed;
        bottom: 20px;
        right: 20px;
        background: white;
        border-radius: 12px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        padding: 12px 16px;
        cursor: pointer;
        z-index: 9999;
        display: flex;
        align-items: center;
        min-width: 250px;
        border: 1px solid #eee;
        transition: transform 0.2s;
        animation: slideUp 0.3s ease-out;
    }
    .overlay-container:hover { transform: translateY(-2px); }
    .content { display: flex; align-items: center; width: 100%; gap: 12px; }
    .icon { font-size: 1.5rem; background: #FFF3E0; padding: 8px; border-radius: 50%; }
    .info { display: flex; flex-direction: column; flex: 1; }
    .status { font-weight: bold; color: #E65100; font-size: 0.9rem; }
    .restaurant { font-size: 0.8rem; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 150px; }
    .arrow { font-size: 1.5rem; color: #ccc; }
    
    @keyframes slideUp {
        from { transform: translateY(100%); opacity: 0; }
        to { transform: translateY(0); opacity: 1; }
    }
  `]
})
export class ActiveOrderOverlayComponent {
    trackerService = inject(OrderTrackerService);
    order = this.trackerService.currentOrder;
    showModal = signal(false);

    openModal() {
        this.showModal.set(true);
    }

    closeModal() {
        this.showModal.set(false);
    }
}
