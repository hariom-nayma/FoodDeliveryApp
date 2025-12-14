import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, FormArray } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RestaurantService } from '../../core/services/restaurant.service';

@Component({
  selector: 'app-restaurant-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatSnackBarModule],
  template: `
    <div class="settings-container">
      <div class="header">
        <h1>Restaurant Settings</h1>
        <p>Manage your restaurant details and appearance</p>
      </div>

      @if (loading()) {
        <div class="loading">Loading settings...</div>
      } @else {
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          
          <div class="section">
            <h3>Basic Info</h3>
            <div class="form-group">
              <label>Restaurant Name</label>
              <input type="text" formControlName="name">
              @if(form.get('name')?.invalid && form.get('name')?.touched) {
                  <span class="error">Name is required</span>
              }
            </div>

            <div class="form-group">
              <label>Description</label>
              <textarea formControlName="description" rows="3"></textarea>
            </div>

            <div class="form-group">
                <label>Logo URL</label>
                <input type="text" formControlName="imageUrl" placeholder="https://example.com/logo.png">
                @if(form.get('imageUrl')?.value) {
                    <div class="preview">
                        <img [src]="form.get('imageUrl')?.value" alt="Logo Preview" onerror="this.src='/assets/placeholder.png'">
                    </div>
                }
            </div>
          </div>

          <div class="section">
            <h3>Contact & Timings</h3>
            <div class="form-group">
              <label>Phone Number</label>
              <input type="text" formControlName="phone">
            </div>

            <div class="row">
                <div class="form-group half">
                  <label>Opening Time</label>
                  <input type="time" formControlName="openingTime">
                </div>
                <div class="form-group half">
                  <label>Closing Time</label>
                  <input type="time" formControlName="closingTime">
                </div>
            </div>
          </div>

          <div class="section">
            <h3>Cuisines</h3>
            <div class="form-group">
                <label>Cuisine Types (Comma separated)</label>
                <input type="text" [value]="cuisineStr" (input)="updateCuisines($event)" placeholder="Italian, Chinese, Fast Food">
            </div>
          </div>

          <button type="submit" class="btn-save" [disabled]="form.invalid || saving()">
            {{ saving() ? 'Saving...' : 'Save Changes' }}
          </button>
        </form>
      }
    </div>
  `,
  styles: [`
    .settings-container { max-width: 800px; margin: 0 auto; padding-bottom: 50px; }
    .header { margin-bottom: 2rem; border-bottom: 1px solid #eee; padding-bottom: 1rem; }
    .header h1 { margin: 0 0 0.5rem 0; color: #333; }
    .header p { margin: 0; color: #666; }
    
    .section { background: white; padding: 1.5rem; border-radius: 8px; border: 1px solid #eee; margin-bottom: 1.5rem; }
    h3 { margin-top: 0; margin-bottom: 1.5rem; color: #444; border-bottom: 1px solid #f9f9f9; padding-bottom: 0.5rem; }
    
    .form-group { margin-bottom: 1.2rem; }
    label { display: block; margin-bottom: 0.5rem; font-weight: 500; color: #555; }
    input, textarea { width: 100%; padding: 10px; border: 1px solid #ddd; border-radius: 6px; font-size: 1rem; }
    input:focus, textarea:focus { border-color: #e23744; outline: none; }
    textarea { resize: vertical; }
    
    .row { display: flex; gap: 1rem; }
    .half { flex: 1; }
    
    .preview { margin-top: 10px; }
    .preview img { width: 80px; height: 80px; object-fit: cover; border-radius: 8px; border: 1px solid #eee; }
    
    .btn-save { background: #e23744; color: white; border: none; padding: 12px 24px; border-radius: 6px; font-size: 1rem; cursor: pointer; float: right; }
    .btn-save:disabled { opacity: 0.7; cursor: not-allowed; }
    
    .error { color: red; font-size: 0.85rem; margin-top: 5px; display: block; }
    .loading { text-align: center; color: #666; margin-top: 50px; }
  `]
})
export class RestaurantSettingsComponent implements OnInit {
  fb = inject(FormBuilder);
  route = inject(ActivatedRoute);
  restaurantService = inject(RestaurantService);
  snack = inject(MatSnackBar);

  restaurantId = '';
  loading = signal(true);
  saving = signal(false);
  cuisineStr = '';

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    phone: ['', Validators.required],
    openingTime: ['', Validators.required],
    closingTime: ['', Validators.required],
    imageUrl: [''],
    cuisineTypes: [[] as string[]]
  });

  ngOnInit() {
    this.route.parent?.params.subscribe(params => {
      this.restaurantId = params['id'];
      this.loadRestaurant();
    });
  }

  loadRestaurant() {
    this.restaurantService.getRestaurant(this.restaurantId).subscribe({
      next: (data: any) => { // Using any temporarily or matching Restaurant type if updated
        this.form.patchValue({
          name: data.name,
          description: data.description,
          phone: data.phone,
          openingTime: data.openingTime,
          closingTime: data.closingTime,
          imageUrl: data.imageUrl || ''
        });

        // Handle cuisines
        if (data.cuisineTypes) {
          this.form.patchValue({ cuisineTypes: data.cuisineTypes });
          this.cuisineStr = data.cuisineTypes.join(', ');
        }

        this.loading.set(false);
      },
      error: (err: any) => {
        console.error(err);
        this.loading.set(false);
        this.snack.open('Failed to load restaurant details', 'Close', { duration: 3000 });
      }
    });
  }

  updateCuisines(event: any) {
    const val = event.target.value;
    this.cuisineStr = val;
    const arr = val.split(',').map((s: string) => s.trim()).filter((s: string) => s.length > 0);
    this.form.patchValue({ cuisineTypes: arr });
  }

  onSubmit() {
    if (this.form.invalid) return;

    this.saving.set(true);
    const data = this.form.value;

    this.restaurantService.updateRestaurant(this.restaurantId, data).subscribe({
      next: (res: any) => {
        this.snack.open('Restaurant settings updated successfully', 'Close', { duration: 3000 });
        this.saving.set(false);
      },
      error: (err: { error: { message: any; }; }) => {
        console.error(err);
        this.snack.open(err.error?.message || 'Failed to update settings', 'Close', { duration: 3000 });
        this.saving.set(false);
      }
    });
  }
}
