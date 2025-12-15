import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class DeliveryPartnerService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/delivery-partners`;

  register(formData: FormData): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, formData);
  }

  getProfile(): Observable<any> {
    return this.http.get(`${this.apiUrl}/profile`);
  }

  goOnline(lat: number, lng: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/status/online`, { latitude: lat, longitude: lng });
  }

  goOffline(): Observable<any> {
    return this.http.patch(`${this.apiUrl}/status/offline`, {});
  }

  updateLocation(lat: number, lng: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/location`, { latitude: lat, longitude: lng });
  }

  // Order Management (Base: /api/v1/delivery/orders)
  private ordersUrl = `${environment.apiUrl}/delivery/orders`;

  getRequests(): Observable<any> {
    return this.http.get(`${this.ordersUrl}/requests`);
  }

  respondToAssignment(assignmentId: string, accepted: boolean): Observable<any> {
    return this.http.post(`${this.ordersUrl}/requests/${assignmentId}/respond`, { accepted });
  }

  markPickedUp(orderId: string): Observable<any> {
    return this.http.patch(`${this.ordersUrl}/${orderId}/picked-up`, {});
  }

  markDelivered(orderId: string): Observable<any> {
    return this.http.patch(`${this.ordersUrl}/${orderId}/delivered`, {});
  }

  getHistory(): Observable<any> {
    return this.http.get(`${this.ordersUrl}/history`);
  }

  getActiveOrders(): Observable<any> {
    return this.http.get(`${this.ordersUrl}/active`);
  }

  getDailyEarnings(): Observable<any> {
    return this.http.get(`${this.ordersUrl}/earnings/daily`);
  }
}
