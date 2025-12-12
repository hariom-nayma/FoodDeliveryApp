import { Component, effect, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeliveryPartnerService } from '../../core/services/delivery-partner.service';
import { SocketService } from '../../core/services/socket.service';
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
  private socketService = inject(SocketService); // Inject SocketService

  isOnline = signal(false);
  activeTab = signal<'requests' | 'active' | 'history' | 'earnings'>('requests');

  requests = signal<any[]>([]);
  incomingRequest = signal<any>(null); // Real-time popup request
  currentOrder = signal<any>(null); // Simplified for single active order
  history = signal<any[]>([]);
  earnings = signal<any>(null);
  userId = signal<string>(''); // Store userId

  locationInterval: Subscription | null = null;
  // requestsInterval: Subscription | null = null; // Removed polling as per request
  timeLeft = signal(0);
  private timerInterval: any = null;

  message = signal('');

  ngOnInit() {
    this.refreshProfile();

    // Listen for Real-time Requests
    this.socketService.onAssignmentRequest().subscribe(payload => {
      console.log("New Assignment Request:", payload);
      // Play sound if needed
      this.playSound();

      this.incomingRequest.set(payload);
      this.startCountdown(25); // 25 seconds to accept
    });
  }

  startCountdown(seconds: number) {
    this.stopCountdown();
    this.timeLeft.set(seconds);

    this.timerInterval = setInterval(() => {
      this.timeLeft.update(t => t - 1);
      if (this.timeLeft() <= 0) {
        this.stopCountdown();
        this.incomingRequest.set(null); // Auto-dismiss
        // Optional: Auto-reject via API if needed, or just let backend timeout
      }
    }, 1000);
  }

  stopCountdown() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  playSound() {
    const audio = new Audio('assets/notification.mp3'); // Ensure this file exists or use a generic URL
    audio.play().catch(e => console.log('Audio play failed', e));
  }

  refreshProfile() {
    this.deliveryService.getProfile().subscribe(res => {
      this.isOnline.set(res.data.isOnline);
      if (this.isOnline()) {
        this.startBackgroundTasks();
      }
      this.checkForActiveOrders();

      // Join Room
      if (res.data.userId) { // Ensure profile returns userId
        this.socketService.joinRiderRoom(res.data.userId);
      }
    });
  }

  checkForActiveOrders() {
    this.deliveryService.getActiveOrders().subscribe(res => {
      if (res.data && res.data.length > 0) {
        const active = res.data[0];
        this.currentOrder.set(active);
        this.activeTab.set('active');
        this.message.set('Resumed active order 📦');
      }
    });
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
    // Poll Location every 15s via Socket
    this.locationInterval = interval(15000).subscribe(() => {
      if (!this.userId()) return; // Wait for userId
      navigator.geolocation.getCurrentPosition(pos => {
        // Socket Emission instead of API Call
        this.socketService.emitLocation(this.userId(), pos.coords.latitude, pos.coords.longitude);
      });
    });

    // Removed polling requestsInterval as requested
    // Initial fetch
    this.fetchRequests();
  }

  stopBackgroundTasks() {
    if (this.locationInterval) this.locationInterval.unsubscribe();
    // if (this.requestsInterval) this.requestsInterval.unsubscribe();
    this.stopCountdown();
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
    this.stopCountdown();
    this.deliveryService.respondToAssignment(assignmentId, true).subscribe(res => {
      this.message.set('Order Accepted! 🚀');
      this.currentOrder.set(res.data); // Or fetch active
      this.activeTab.set('active');
      this.incomingRequest.set(null); // Clear popup
      this.fetchRequests();
    }, err => {
      this.message.set('Failed to accept: ' + (err.error?.message || 'Unknown error'));
      this.fetchRequests();
    });
  }

  rejectOrder(assignmentId: string) {
    this.deliveryService.respondToAssignment(assignmentId, false).subscribe(() => {
      this.message.set('Order Rejected ❌');
      this.fetchRequests();
    });
  }

  // Real-time Popup Actions
  acceptIncoming() {
    if (!this.incomingRequest()) return;
    const id = this.incomingRequest().assignmentId;
    this.acceptOrder(id);
    this.incomingRequest.set(null);
  }

  rejectIncoming() {
    if (!this.incomingRequest()) return;
    const id = this.incomingRequest().assignmentId;
    this.rejectOrder(id);
    this.incomingRequest.set(null);
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
