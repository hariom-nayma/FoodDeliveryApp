import { Component, effect, inject, signal, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DeliveryPartnerService } from '../../core/services/delivery-partner.service';
import { SocketService } from '../../core/services/socket.service';
import { NavigationService } from '../../services/navigation.service';
import { MapComponent } from '../../shared/components/map/map.component';
import { interval, Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

@Component({
  selector: 'app-delivery-partner-home',
  standalone: true,
  imports: [CommonModule, MapComponent],
  templateUrl: './delivery-partner-home.html',
  styleUrl: './delivery-partner-home.css'
})
export class DeliveryPartnerHome implements OnInit, OnDestroy {
  private deliveryService = inject(DeliveryPartnerService);
  private socketService = inject(SocketService); // Inject SocketService
  private navigationService = inject(NavigationService);

  @ViewChild(MapComponent) set map(content: MapComponent) {
    if (content) {
      this.mapComponent = content;
      console.log('Map Component Loaded inside ViewChild');
      setTimeout(() => content.initializeMap(), 0);
    }
  }
  mapComponent!: MapComponent;

  // ...

  // initMap removed, logic handled by setter


  isOnline = signal(false);
  activeTab = signal<'requests' | 'active' | 'history' | 'earnings'>('requests');

  requests = signal<any[]>([]);
  incomingRequest = signal<any>(null); // Real-time popup request
  currentOrder = signal<any>(null); // Simplified for single active order
  history = signal<any[]>([]);
  earnings = signal<any>(null);
  userId = signal<string>(''); // Store userId
  riderLocation = signal<{ lat: number, lng: number } | null>(null);
  
  constructor() {
    effect(() => {
        const order = this.currentOrder();
        const loc = this.riderLocation();
        console.log('Effect Triggered: Order=', !!order, 'Location=', loc);
        
        // Redraw route if map is ready and data changed
        if (order && loc && this.mapComponent) {
             console.log('Effect: Conditions met, drawing route in 100ms');
             setTimeout(() => this.updateRoute(order, loc), 100);
        } else {
             console.log('Effect: Conditions NOT met. MapComponent=', !!this.mapComponent);
        }
    });
  }

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

    this.socketService.onOrderUpdate().subscribe(payload => {
      console.log("Socket Order Update:", payload);
      if (payload.status === 'ASSIGNED_TO_RIDER') {
        this.currentOrder.set(payload);
        this.activeTab.set('active');
        if (this.incomingRequest() && this.incomingRequest().orderId === payload.orderId) {
          this.incomingRequest.set(null); // Clear request if accepted elsewhere
        }
      } else if (payload.status === 'PICKED_UP') {
        this.currentOrder.set(payload);
      } else if (payload.status === 'DELIVERED') {
        this.currentOrder.set(null);
        this.activeTab.set('requests');
        this.message.set("Order Completed!");
      }
      this.onMapReady();
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
      console.log('Profile Refreshed:', res.data);
      this.isOnline.set(res.data.isOnline);
      
      // CRITICAL FIX: Set userId signal
      if (res.data.userId) {
          this.userId.set(res.data.userId); 
      } else if (res.data.user && res.data.user.id) {
          this.userId.set(res.data.user.id);
      } else {
          // Fallback if DTO is different, or log error
          console.error('Profile missing userId!', res.data);
      }

      if (this.isOnline()) {
        this.startBackgroundTasks();
      }
      this.checkForActiveOrders();

      // Join Room
      if (this.userId()) { 
        this.socketService.joinRiderRoom(this.userId());
      }
    });
  }

  checkForActiveOrders() {
    this.deliveryService.getActiveOrders().subscribe(res => {
      console.log('CheckForActiveOrders Response:', res);
      if (res.data && res.data.length > 0) {
        const active = res.data[0];
        this.currentOrder.set(active);
        this.activeTab.set('active');
        this.message.set('Resumed active order 📦');
      } else {
        console.log('No active orders found in response');
      }
    }, err => console.error('CheckForActiveOrders Error:', err));
  }

  updateRoute(order: any, riderLoc: { lat: number, lng: number }) {
    console.log('Updating Route:', order, riderLoc);
    let fromLat = riderLoc.lat;
    let fromLng = riderLoc.lng;
    let toLat = 0;
    let toLng = 0;

    if (order.status === 'ASSIGNED_TO_RIDER') {
      // Route to Restaurant
      if (order.pickupLocation) {
        toLat = order.pickupLocation.lat;
        toLng = order.pickupLocation.lng;
      }
    } else if (order.status === 'PICKED_UP') {
      // Route to Customer
      if (order.dropLocation) {
        toLat = order.dropLocation.lat;
        toLng = order.dropLocation.lng;
      }
    }

    console.log('Route Coords:', fromLat, fromLng, '->', toLat, toLng);

    if (toLat && toLng) {
      console.log('DeliveryPartnerHome: calling getRoute', fromLat, fromLng, toLat, toLng);
      
      const markers = [
          { lat: fromLat, lng: fromLng, title: 'You', icon: 'assets/deliveryman-marker.png' }
      ];

      if (order.status === 'ASSIGNED_TO_RIDER') {
          markers.push({ lat: toLat, lng: toLng, title: order.restaurantName, icon: 'assets/restaurant-marker.png' });
      } else {
          markers.push({ lat: toLat, lng: toLng, title: 'Customer', icon: 'assets/deliverlocation-marker.png' });
      }

      this.mapComponent.updateMarkers(markers);

      this.navigationService.getRoute(fromLat, fromLng, toLat, toLng).subscribe({
        next: (res) => {
          console.log('Route Response:', res);
          if (res.data && res.data.polyline) {
            console.log('Drawing Route on Map...');
            this.mapComponent.drawRoute(res.data.polyline);
          } else {
             console.warn('Route response missing polyline:', res);
          }
        },
        error: (err) => {
            console.error('Route Error:', err);
            this.message.set('Failed to load route: ' + (err.error?.message || err.message));
        }
      });
    } else {
        console.warn('DeliveryPartnerHome: Missing coordinates', fromLat, fromLng, toLat, toLng);
    }
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
    console.log('Starting background tasks...');
    // Poll Location every 15s via Socket, starting immediately (0)
    this.locationInterval = timer(0, 15000).subscribe(() => {
      if (!this.userId()) {
          console.warn('Skipping location fetch: No userId');
          return; 
      }
      console.log('Requesting Geolocation...');
      navigator.geolocation.getCurrentPosition(pos => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        console.log('Rider Location Acquired:', lat, lng);
        this.riderLocation.set({ lat, lng }); // Update signal

        // Socket Emission instead of API Call
        this.socketService.emitLocation(this.userId(), lat, lng);
      }, err => {
          console.error('Geolocation Error:', err.message, err.code);
          this.message.set('Location Error: ' + err.message);
      }, {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 0
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



  onMapReady() {
    console.log('Map Ready Event Received');
    const order = this.currentOrder();
    const riderLoc = this.riderLocation();
    if (order && riderLoc) {
      this.updateRoute(order, riderLoc);
    }
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
