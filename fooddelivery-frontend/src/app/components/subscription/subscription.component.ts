import { Component, NgZone, computed } from '@angular/core';
import { CommonModule, formatDate } from '@angular/common';
import { UserService } from '../../core/services/user.service';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

declare var Razorpay: any;

@Component({
  selector: 'app-subscription',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './subscription.component.html',
  styleUrl: './subscription.component.css'
})
export class SubscriptionComponent {
    
    isLoading = false;
    errorMsg = '';
    
    // Computed signals ensuring reactive updates without manual checks
    isPremium = computed(() => {
        const user = this.authService.currentUser();
        if (user && user.premiumExpiry) {
            return new Date(user.premiumExpiry).getTime() > Date.now();
        }
        return false;
    });

    expiryDate = computed(() => {
        const user = this.authService.currentUser();
        if (user && user.premiumExpiry) {
            console.log(user.premiumExpiry);
         let expiryDate = new Date(user.premiumExpiry);
         return formatDate(expiryDate, 'dd/MM/yyyy', 'en-US');
        }
        return '';
    });
    
    constructor(
        private userService: UserService, 
        private router: Router, 
        private ngZone: NgZone,
        private authService: AuthService
    ) {
        this.refreshProfile();
    }

    refreshProfile() {
        this.userService.getUserProfile().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.authService.currentUser.set(res.data);
                    localStorage.setItem('user', JSON.stringify(res.data));
                }
            },
            error: (err) => console.error('Profile refresh failed', err)
        });
    }

    buyPremium() {
        this.isLoading = true;
        this.errorMsg = '';
        
        this.userService.initiateSubscription().subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.openRazorpay(res.data);
                } else {
                    this.errorMsg = res.message || 'Failed to initiate subscription.';
                    this.isLoading = false;
                }
            },
            error: (err) => {
                this.errorMsg = 'Error initiating subscription.';
                this.isLoading = false;
                console.error(err);
            }
        });
    }

    openRazorpay(data: any) {
        const options = {
            key: 'rzp_test_RkLdNNDedmJSMf',
            amount: data.amount,
            currency: data.currency,
            name: 'Food Delivery Premium',
            description: '3 Months Premium Membership',
            order_id: data.razorpayOrderId,
            handler: (response: any) => {
                this.verifyPayment({
                    razorpayOrderId: response.razorpay_order_id,
                    razorpayPaymentId: response.razorpay_payment_id,
                    razorpaySignature: response.razorpay_signature
                });
            },
            prefill: {},
            theme: {
                color: '#EF4F5F'
            }
        };

        const rzp = new Razorpay(options);
        rzp.open();
        
        rzp.on('payment.failed', (response: any) => {
            this.ngZone.run(() => {
                this.errorMsg = 'Payment Failed: ' + response.error.description;
                this.isLoading = false;
            });
        });
    }

    verifyPayment(payload: any) {
        this.userService.verifySubscription(payload).subscribe({
            next: (res) => {
                this.ngZone.run(() => {
                    if (res.success) {
                        alert('Premium Activated Successfully!');
                        this.refreshProfile();
                    } else {
                        this.errorMsg = 'Verification Failed.';
                    }
                    this.isLoading = false;
                });
            },
            error: (err) => {
               this.ngZone.run(() => {
                    this.errorMsg = 'Verification Error.';
                    this.isLoading = false;
               });
            }
        });
    }
}
