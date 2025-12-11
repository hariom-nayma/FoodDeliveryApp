import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { RestaurantService } from '../core/restaurant/restaurant.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [ReactiveFormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent {
    private fb = inject(NonNullableFormBuilder);
    private authService = inject(AuthService);
    private restaurantService = inject(RestaurantService);
    private router = inject(Router);

    form = this.fb.group({
        email: ['', [Validators.required, Validators.email]],
        password: ['', Validators.required]
    });

    loading = signal(false);
    error = signal('');

    onSubmit() {
        if (this.form.invalid) return;

        this.loading.set(true);
        this.error.set('');

        this.authService.login(this.form.getRawValue()).subscribe({
            next: (response) => {
                const user = response.data.user;
                console.log('Login successful, role:', user?.role);

                if (user?.role === 'ROLE_RESTAURANT_OWNER') {
                    this.restaurantService.getMyRestaurants().subscribe({
                        next: (restaurant) => {
                            if (restaurant) {
                                this.router.navigate(['/restaurant/manage', restaurant.id, 'dashboard']);
                            } else {
                                this.router.navigate(['/']);
                            }
                        },
                        error: () => this.router.navigate(['/'])
                    });
                } else if (user?.role === 'ROLE_DELIVERY_PARTNER') {
                    this.router.navigate(['/partner/home']);
                } else {
                    this.router.navigate(['/']);
                }
            },
            error: (err) => {
                this.error.set('Invalid email or password');
                this.loading.set(false);
            }
        });
    }
}
