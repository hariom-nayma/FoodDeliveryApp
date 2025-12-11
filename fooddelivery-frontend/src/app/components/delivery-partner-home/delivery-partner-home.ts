import { Component, effect, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeliveryPartnerService } from '../../core/services/delivery-partner.service';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-delivery-partner-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './delivery-partner-home.html',
  styleUrl: './delivery-partner-home.css'
})
export class DeliveryPartnerHome implements OnInit, OnDestroy {
  private deliveryService = inject(DeliveryPartnerService);

  isOnline = signal(false);
  activeTab = signal<'requests' | 'active' | 'history' | 'earnings'>('requests');

  requests = signal<any[]>([]);
  currentOrder = signal<any>(null); // Simplified for single active order
  history = signal<any[]>([]);
  earnings = signal<any>(null);

  locationInterval: Subscription | null = null;
  requestsInterval: Subscription | null = null;
  message = signal('');

  ngOnInit() {
    this.refreshProfile();
  }

  refreshProfile() {
    this.deliveryService.getProfile().subscribe(res => {
      this.isOnline.set(res.data.isOnline);
      if (this.isOnline()) {
        this.startBackgroundTasks();
      }
    });

    // Also check for any active order that might be in progress (Not implemented in backend strictly, but good practice)
    // For now, we rely on accepting a request to set currentOrder locally or fetch from "My Assignments" endpoint if existed.
    // I will simplify: If 'active' tab is clicked, fetch assignments. 
    // Actually, `activeTab` logic will fetch data.
  }

  toggleStatus() {
    if (this.isOnline()) {
      this.deliveryService.goOffline().subscribe(() => {
        this.isOnline.set(false);
        this.stopBackgroundTasks();
        this.message.set('You are now Offline 🔴');
      });
    } else {
      // Mock location or get real location
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          this.deliveryService.goOnline(pos.coords.latitude, pos.coords.longitude).subscribe(() => {
            this.isOnline.set(true);
            this.startBackgroundTasks();
            this.message.set('You are now Online 🟢');
          });
        },
        (err) => {
          alert('Location access required to go online.');
        }
      );
    }
  }

  startBackgroundTasks() {
    // Poll Location every 15s
    this.locationInterval = interval(15000).subscribe(() => {
      navigator.geolocation.getCurrentPosition(pos => {
        this.deliveryService.updateLocation(pos.coords.latitude, pos.coords.longitude).subscribe();
      });
    });

    // Poll Requests every 15s
    this.fetchRequests();
    this.requestsInterval = interval(10000).subscribe(() => {
      if (this.activeTab() === 'requests') this.fetchRequests();
    });
  }

  stopBackgroundTasks() {
    if (this.locationInterval) this.locationInterval.unsubscribe();
    if (this.requestsInterval) this.requestsInterval.unsubscribe();
  }

  ngOnDestroy() {
    this.stopBackgroundTasks();
  }

  setTab(tab: 'requests' | 'active' | 'history' | 'earnings') {
    this.activeTab.set(tab);
    if (tab === 'requests') this.fetchRequests();
    if (tab === 'history') this.fetchHistory();
    if (tab === 'earnings') this.fetchEarnings();
  }

  fetchRequests() {
    this.deliveryService.getRequests().subscribe(res => {
      this.requests.set(res.data);
    });
  }

  acceptOrder(assignmentId: string) {
    this.deliveryService.acceptOrder(assignmentId).subscribe(res => {
      this.message.set('Order Accepted! 🚀');
      this.currentOrder.set(res.data);
      this.activeTab.set('active');
      this.fetchRequests(); // clear from list
    });
  }

  rejectOrder(assignmentId: string) {
    this.deliveryService.rejectOrder(assignmentId).subscribe(() => {
      this.fetchRequests();
    });
  }

  markPickedUp() {
    if (!this.currentOrder()) return;
    this.deliveryService.markPickedUp(this.currentOrder().orderId).subscribe(res => {
      // Update local status
      this.currentOrder.update(o => ({ ...o, status: 'PICKED_UP' }));
    });
  }

  markDelivered() {
    if (!this.currentOrder()) return;
    this.deliveryService.markDelivered(this.currentOrder().orderId).subscribe(res => {
      this.message.set('Order Delivered! 🎉');
      this.currentOrder.set(null); // Clear active
      this.activeTab.set('requests');
    });
  }

  fetchHistory() {
    this.deliveryService.getHistory().subscribe(res => this.history.set(res.data));
  }

  fetchEarnings() {
    this.deliveryService.getDailyEarnings().subscribe(res => this.earnings.set(res.data));
  }
}
