import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { AddressService, Address } from '../../core/services/address.service';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';

@Component({
    selector: 'app-cart',
    standalone: true,
    imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule],
    templateUrl: './cart.component.html',
    styleUrl: './cart.component.css'
})
export class CartComponent implements OnInit {
    cartService = inject(CartService);
    addressService = inject(AddressService);
    orderService = inject(OrderService);
    authService = inject(AuthService);
    router = inject(Router);

    addresses = signal<any[]>([]);
    selectedAddressId = signal<string>('');
    paymentMethod = signal<string>('COD');
    placingOrder = signal(false);
    pricing = signal<any>(null);

    ngOnInit() {
        this.addressService.getMyAddresses().subscribe((addrs: any[]) => {
            this.addresses.set(addrs);
            if (addrs.length > 0) {
                this.selectedAddressId.set(addrs[0].addressId);
                // Initial fetch
                this.fetchPrice(); 
            }
        });
    }

    fetchPrice() {
        const cart = this.cartService.cart();
        const addressId = this.selectedAddressId();

        if (!cart || cart.items.length === 0 || !addressId) {
            this.pricing.set(null);
            return;
        }

        const payload = {
            restaurantId: cart.restaurantId,
            items: cart.items.map(i => ({
                itemId: i.itemId,
                quantity: i.quantity,
                options: i.options ? i.options.map((o: any) => ({ optionId: 'UNKNOWN' })) : [] 
                // Note: CartItemResponse structure verification required for exact option mapping.
                // If backend requires precise option IDs for price calc, they must be present in CartItemResponse.
            })),
            deliveryAddressId: addressId,
            userId: this.authService.currentUser()?.id
        };

        this.cartService.calculatePrice(payload).subscribe({
            next: (res) => {
                if (res.success) {
                    this.pricing.set(res.data);
                }
            },
            error: (err) => console.error('Pricing Error', err)
        });
    }

    selectAddress(id: string) {
        this.selectedAddressId.set(id);
        this.fetchPrice();
    }

    updateQty(cartItemId: string, newQty: number) {
        if (newQty <= 0) {
            this.removeItem(cartItemId);
            return;
        }
        this.cartService.updateCartItem({ cartItemId, quantity: newQty }).subscribe(() => this.fetchPrice());
    }

    removeItem(cartItemId: string) {
        if (confirm('Remove this item?')) {
            this.cartService.removeCartItem(cartItemId).subscribe(() => this.fetchPrice());
        }
    }

    clearCart() {
        if (confirm('Clear entire cart?')) {
            this.cartService.clearCart().subscribe(() => this.pricing.set(null));
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
        this.pricing.set(null);
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
                name: 'Guest',
                contact: '9999999999'
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
