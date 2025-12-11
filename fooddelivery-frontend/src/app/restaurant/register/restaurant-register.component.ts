import { Component, inject, signal, OnInit } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RestaurantService } from '../../core/restaurant/restaurant.service';
import { RestaurantRequest } from '../../core/restaurant/restaurant.types';

@Component({
    selector: 'app-restaurant-register',
    standalone: true,
    imports: [ReactiveFormsModule],
    templateUrl: './restaurant-register.component.html',
    styles: [`
    .container { max-width: 800px; margin: 2rem auto; padding: 1rem; }
    .card { background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
    h2 { margin-bottom: 2rem; color: #333; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .col-full { grid-column: 1 / -1; }
    label { display: block; margin-bottom: 0.5rem; color: #666; font-weight: 500; }
    input, textarea, select { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; margin-bottom: 1rem; }
    button { background: var(--primary-color); color: white; border: none; padding: 1rem; border-radius: 4px; width: 100%; font-size: 1rem; cursor: pointer; }
    button:disabled { background: #ccc; cursor: not-allowed; }
    .error { color: red; font-size: 0.875rem; margin-top: -0.5rem; margin-bottom: 1rem; }
  `]
})
export class RestaurantRegisterComponent implements OnInit {
    private fb = inject(NonNullableFormBuilder);
    private restaurantService = inject(RestaurantService);
    private router = inject(Router);

    form = this.fb.group({
        name: ['', [Validators.required, Validators.minLength(3)]],
        description: [''],
        phone: ['', [Validators.required, Validators.pattern('^[0-9]{10}$')]],
        email: ['', [Validators.required, Validators.email]],
        cuisineTypes: ['', Validators.required], // Comma separated for simplicity in UI, converted to array
        openingTime: ['10:00', Validators.required],
        closingTime: ['23:00', Validators.required],
        address: this.fb.group({
            addressLine1: ['', Validators.required],
            city: ['', Validators.required],
            state: ['', Validators.required],
            pincode: ['', Validators.required],
            latitude: [0, Validators.required],
            longitude: [0, Validators.required]
        })
    });

    loading = signal(false);

    ngOnInit() {
        this.restaurantService.getMyRestaurants().subscribe({
            next: (res: any) => {
                // res might be null/undefined if 204
                if (!res) return;

                if (res.status === 'PENDING_REVIEW') {
                    this.router.navigate(['/restaurant', 'pending']);
                } else if (res.status === 'DRAFT') {
                    // Optional: User might want to continue draft.
                    // For now, let's allow them to stay on register to create new? 
                    // Or redirect to documents if they finished step 1?
                    // User Requirement: "create an api to check it and use it before calling create Restaurant".
                    // It implies preventing creating a NEW one if one exists.
                    // I'll leave DRAFT handling generic or redirect to dashboard/docs to finish.
                    // But definitely PENDING -> pending page.
                    if (res.gstNumber) {
                        // Likely has basic info
                        // this.router.navigate(['/restaurant', res.id, 'documents']);
                    }
                } else if (res.status === 'APPROVED' || res.status === 'ACTIVE') {
                    this.router.navigate(['/restaurant/manage', res.id, 'dashboard']);
                }
            },
            error: () => { } // Ignore 404/No Content
        });
    }

    error = signal('');
    currentStep = signal(1);

    nextStep() {
        const step = this.currentStep();
        if (step === 1) {
            if (this.checkControls(['name', 'description', 'phone', 'email'])) this.currentStep.set(2);
        } else if (step === 2) {
            if (this.checkControls(['cuisineTypes', 'openingTime', 'closingTime'])) this.currentStep.set(3);
        }
    }

    prevStep() {
        this.currentStep.update(s => Math.max(1, s - 1));
    }

    checkControls(controls: string[]): boolean {
        let valid = true;
        for (const name of controls) {
            const control = this.form.get(name);
            if (control?.invalid) {
                control.markAsTouched();
                valid = false;
            }
        }
        return valid;
    }

    onSubmit() {
        if (this.currentStep() !== 3) return;
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        this.loading.set(true);
        this.error.set('');

        const formValue = this.form.getRawValue();
        const cuisines = formValue.cuisineTypes.split(',').map(c => c.trim()).filter(c => c);

        const request: RestaurantRequest = {
            ...formValue,
            cuisineTypes: cuisines,
            address: formValue.address
        };

        this.restaurantService.createRestaurant(request).subscribe({
            next: (restaurant) => {
                this.router.navigate(['/restaurant', restaurant.id, 'documents']);
            },
            error: (err) => {
                this.error.set(err.error?.message || 'Failed to create restaurant');
                this.loading.set(false);
            }
        });
    }
}
