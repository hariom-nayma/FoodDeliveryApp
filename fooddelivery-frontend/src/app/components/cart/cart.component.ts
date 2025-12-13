import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AddressService, Address } from '../../core/services/address.service';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

@Component({
    selector: 'app-cart',
    standalone: true,
    imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
    template: `
    <div class="container cart-container">
      <h1>Your Cart</h1>
      
      @if (cartService.cart(); as cart) {
        @if (cart.items.length > 0) {
          <div class="cart-layout">
            <div class="left-section">
                <!-- Cart Items -->
                <div class="cart-items">
                    <div class="cart-header">
                        <span>Item</span>
                        <span>Qty</span>
                        <span>Price</span>
                        <span>Total</span>
                        <span>Action</span>
                    </div>
                    
                    @for (item of cart.items; track item.cartItemId) {
                        <div class="cart-item">
                            <div class="item-details">
                                <h4>{{ item.name }}</h4>
                                <small *ngIf="item.options.length > 0">
                                    <span *ngFor="let opt of item.options">{{ opt.groupName }}: {{ opt.optionSelected }} (+₹{{opt.extraPrice}}), </span>
                                </small>
                            </div>
                            <div class="qty-control">
                                <button (click)="updateQty(item.cartItemId, item.quantity - 1)">-</button>
                                <span>{{ item.quantity }}</span>
                                <button (click)="updateQty(item.cartItemId, item.quantity + 1)">+</button>
                            </div>
                            <div class="price">₹{{ item.basePrice }}</div>
                            <div class="total">₹{{ item.finalPrice }}</div>
                            <div class="action">
                                <button class="remove-btn" (click)="removeItem(item.cartItemId)">🗑️</button>
                            </div>
                        </div>
                    }

                    <div class="cart-actions">
                        <button class="clear-btn" (click)="clearCart()">Clear Cart</button>
                    </div>
                </div>

                <!-- Address Selection -->
                <div class="section-card mt-6">
                    <h3>Delivery Address</h3>
                    @if (addresses().length > 0) {
                        <div class="address-list">
                            <div *ngFor="let addr of addresses()" class="address-option" 
                                 [class.selected]="selectedAddressId() === addr.addressId"
                                 (click)="selectedAddressId.set(addr.addressId)">
                                <input type="radio" [name]="'address'" [checked]="selectedAddressId() === addr.addressId">
                                <div class="addr-details">
                                    <strong>{{ addr.label || addr.type || 'Home' }}</strong>
                                    <p>{{ addr.line1 }}, {{ addr.city }} - {{ addr.postalCode }}</p>
                                </div>
                            </div>
                        </div>
                    } @else {
                        <p>No saved addresses. <a routerLink="/profile/addresses" class="text-blue-600">Add one</a></p>
                    }
                </div>

                <!-- Payment Method -->
                <div class="section-card mt-6">
                    <h3>Payment Method</h3>
                    <div class="payment-options">
                        <select [(ngModel)]="paymentMethod" class="form-select">
                            <option value="COD">Cash On Delivery (COD)</option>
                            <option value="ONLINE">Online Payment (UPI/Card)</option>
                        </select>
                    </div>
                </div>
            </div>

            <div class="cart-summary">
                <h3>Bill Details</h3>
                <div class="summary-row">
                    <span>Item Total</span>
                    <span>₹{{ cart.subtotal }}</span>
                </div>
                <div class="summary-row">
                    <span>Delivery Fee</span>
                    <span>₹{{ cart.deliveryFee }}</span>
                </div>
                <div class="summary-row">
                    <span>Taxes</span>
                    <span>₹{{ cart.tax }}</span>
                </div>
                <hr>
                <div class="summary-row total-row">
                    <span>Grand Total</span>
                    <span>₹{{ cart.total }}</span>
                </div>
                
                <button class="checkout-btn" (click)="placeOrder()" [disabled]="!selectedAddressId() || placingOrder()">
                    {{ placingOrder() ? 'Placing Order...' : 'Proceed to Pay' }}
                </button>
                <p *ngIf="!selectedAddressId()" class="text-red-500 text-sm mt-2 text-center">Please select an address</p>
            </div>
          </div>
        } @else {
            <div class="empty-cart">
                <p>Your cart is empty.</p>
                <a routerLink="/" class="btn btn-primary">Browse Restaurants</a>
            </div>
        }
      } @else {
        <div class="loading">Loading cart...</div>
      }
    </div>
  `,
    styles: [`
    .cart-container { padding: 2rem 0; }
    .cart-layout { display: grid; grid-template-columns: 1fr 350px; gap: 2rem; }
    .cart-header { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr 0.5fr; font-weight: bold; padding: 10px 0; border-bottom: 2px solid #eee; }
    .cart-item { display: grid; grid-template-columns: 2fr 1fr 1fr 1fr 0.5fr; padding: 15px 0; border-bottom: 1px solid #eee; align-items: center; }
    .qty-control { display: flex; gap: 10px; align-items: center; }
    .qty-control button { width: 25px; height: 25px; border: 1px solid #ccc; background: white; cursor: pointer; }
    .remove-btn { background: none; border: none; cursor: pointer; font-size: 1.2rem; }
    .cart-summary { background: #f9f9f9; padding: 20px; border-radius: 8px; height: fit-content; position: sticky; top: 100px; }
    .summary-row { display: flex; justify-content: space-between; margin-bottom: 10px; }
    .total-row { font-weight: bold; font-size: 1.2rem; margin-top: 10px; }
    .checkout-btn { width: 100%; padding: 12px; background: #e23744; color: white; border: none; border-radius: 6px; font-size: 1rem; cursor: pointer; margin-top: 20px; }
    .checkout-btn:disabled { background: #ccc; cursor: not-allowed; }
    .clear-btn { margin-top: 20px; background: none; border: 1px solid #ccc; padding: 8px 16px; cursor: pointer; }
    .empty-cart { text-align: center; margin-top: 50px; }
    .loading { text-align: center; margin-top: 50px; }
    
    .section-card { background: white; border: 1px solid #eee; padding: 1.5rem; border-radius: 8px; margin-top: 1.5rem; }
    .section-card h3 { margin-top: 0; margin-bottom: 1rem; font-size: 1.1rem; color: #333; }
    
    .address-option { display: flex; gap: 1rem; padding: 1rem; border: 1px solid #ddd; border-radius: 6px; margin-bottom: 0.5rem; cursor: pointer; transition: all 0.2s; }
    .address-option:hover { border-color: #e23744; background: #fff5f5; }
    .address-option.selected { border-color: #e23744; background: #fff0f0; }
    .addr-details p { margin: 0; color: #666; font-size: 0.9rem; }
    
    .form-select { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; }
    
    @media (max-width: 768px) {
        .cart-layout { grid-template-columns: 1fr; }
    }
  `]
})
export class CartComponent implements OnInit {
    cartService = inject(CartService);
    addressService = inject(AddressService);
    orderService = inject(OrderService);
    router = inject(Router);

    addresses = signal<any[]>([]);
    selectedAddressId = signal<string>('');
    paymentMethod = signal<string>('COD');
    placingOrder = signal(false);

    ngOnInit() {
        this.addressService.getMyAddresses().subscribe((addrs: any[]) => {
            this.addresses.set(addrs);
            if (addrs.length > 0) this.selectedAddressId.set(addrs[0].addressId);
        });
    }

    updateQty(cartItemId: string, newQty: number) {
        if (newQty <= 0) {
            this.removeItem(cartItemId);
            return;
        }
        this.cartService.updateCartItem({ cartItemId, quantity: newQty }).subscribe();
    }

    removeItem(cartItemId: string) {
        if (confirm('Remove this item?')) {
            this.cartService.removeCartItem(cartItemId).subscribe();
        }
    }

    clearCart() {
        if (confirm('Clear entire cart?')) {
            this.cartService.clearCart().subscribe();
        }
    }

    placeOrder() {
        if (!this.selectedAddressId()) {
            alert('Please select a delivery address');
            return;
        }

        this.placingOrder.set(true);
        const cartId = this.cartService.cart()?.cartId;
        if (!cartId) return;

        this.orderService.createOrder(cartId, this.selectedAddressId(), this.paymentMethod()).subscribe({
            next: (order) => {
                if (order.razorpayOrderId) {
                    this.initRazorpay(order);
                } else {
                    this.finalizeOrder(order.id);
                }
            },
            error: (err) => {
                this.placingOrder.set(false);
                alert('Failed to place order. Please try again.');
            }
        });
    }

    finalizeOrder(orderId: string) {
        this.placingOrder.set(false);
        this.cartService.cart.set(null); // Clear local cart state
        alert('Order Placed Successfully! 🎉');
        this.router.navigate(['/orders', orderId]);
    }

    initRazorpay(order: any) {
        const options = {
            key: 'rzp_test_RkLdNNDedmJSMf',
            amount: order.totalAmount * 100,
            currency: 'INR',
            name: 'Food Delivery App',
            description: 'Order Payment',
            order_id: order.razorpayOrderId,
            handler: (response: any) => {
                this.verifyPayment(order.id, response);
            },
            prefill: {
                name: 'Guest', // Could fetch from profile if available
                contact: '9999999999' // Placeholder or fetch
            },
            theme: {
                color: '#e23744'
            }
        };

        const rzp = new (window as any).Razorpay(options);
        rzp.open();

        rzp.on('payment.failed', (response: any) => {
            this.placingOrder.set(false);
            alert('Payment Failed: ' + response.error.description);
        });
    }

    verifyPayment(orderId: string, response: any) {
        this.orderService.confirmPayment(orderId, response.razorpay_payment_id, response.razorpay_signature)
            .subscribe({
                next: (res) => {
                    this.finalizeOrder(orderId);
                },
                error: (err) => {
                    this.placingOrder.set(false);
                    alert('Payment Verification Failed');
                }
            });
    }
}
