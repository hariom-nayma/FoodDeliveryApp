import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DeliveryPartnerService } from '../../core/services/delivery-partner.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-delivery-partner-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './delivery-partner-signup.html',
  styleUrl: './delivery-partner-signup.css'
})
export class DeliveryPartnerSignup {
  private fb = inject(FormBuilder);
  private deliveryService = inject(DeliveryPartnerService);
  private router = inject(Router);

  signupForm = this.fb.group({
    vehicleType: ['', Validators.required],
    license: [null, Validators.required],
    aadhar: [null, Validators.required],
    rc: [null, Validators.required]
  });

  licenseFile: File | null = null;
  aadharFile: File | null = null;
  rcFile: File | null = null;

  message = signal('');
  isSuccess = signal(false);
  isLoading = signal(false);

  onFileChange(event: Event, field: string) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (field === 'license') this.licenseFile = file;
      if (field === 'aadhar') this.aadharFile = file;
      if (field === 'rc') this.rcFile = file;

      // Update form control to satisfy Validators.required
      this.signupForm.patchValue({ [field]: file });
    }
  }

  selectVehicle(type: string) {
    this.signupForm.patchValue({ vehicleType: type });
    this.signupForm.get('vehicleType')?.markAsDirty();
  }

  onSubmit() {
    console.log('Submit clicked');
    // Check form validity AND manual file checks (redundant but safe)
    if (this.signupForm.invalid || !this.licenseFile || !this.aadharFile || !this.rcFile) {
      console.log('Form invalid', {
        form: this.signupForm.value,
        valid: this.signupForm.valid,
        errors: {
          v: this.signupForm.get('vehicleType')?.errors,
          l: this.signupForm.get('license')?.errors,
          a: this.signupForm.get('aadhar')?.errors,
          r: this.signupForm.get('rc')?.errors
        }
      });
      this.signupForm.markAllAsTouched();
      this.message.set('Please fill all fields and upload all documents.');
      this.isSuccess.set(false);
      return;
    }

    this.isLoading.set(true);
    this.message.set('');

    const formData = new FormData();
    const requestData = { vehicleType: this.signupForm.get('vehicleType')?.value };

    formData.append('data', new Blob([JSON.stringify(requestData)], { type: 'application/json' }));
    formData.append('license', this.licenseFile);
    formData.append('aadhar', this.aadharFile);
    formData.append('rc', this.rcFile);

    this.deliveryService.register(formData).subscribe({
      next: (res) => {
        console.log('Success', res);
        this.isLoading.set(false);
        this.message.set('Application submitted successfully! Please wait for admin approval.');
        this.isSuccess.set(true);
        setTimeout(() => this.router.navigate(['/']), 3000);
      },
      error: (err) => {
        console.error('Error', err);
        this.isLoading.set(false);
        this.message.set(err.error?.message || 'Failed to submit application.');
        this.isSuccess.set(false);
      }
    });
  }
}

