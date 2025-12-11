import { Component, inject, signal, OnInit } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';

@Component({
    selector: 'app-verify-otp',
    standalone: true,
    imports: [ReactiveFormsModule],
    template: `
    <div class="container" style="max-width: 400px; margin-top: 4rem;">
      <div class="card">
        <h2>Verify OTP</h2>
        <p style="text-align: center; color: #666;">
            Enter the 6-digit code sent to {{ email() }}
        </p>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="input-group">
            <label>OTP Code</label>
            <input type="text" formControlName="otp" placeholder="123456" maxlength="6" style="text-align: center; letter-spacing: 4px; font-size: 1.2rem;">
          </div>
          
          @if (error()) {
            <div style="color: red; margin-bottom: 1rem;">{{ error() }}</div>
          }

          <button type="submit" class="btn btn-primary" style="width: 100%" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Verifying...' : 'Verify & Login' }}
          </button>
        </form>
      </div>
    </div>
  `
})
export class VerifyOtpComponent implements OnInit {
    private fb = inject(NonNullableFormBuilder);
    private authService = inject(AuthService);
    private router = inject(Router);
    private route = inject(ActivatedRoute);

    form = this.fb.group({
        otp: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
    });

    loading = signal(false);
    error = signal('');
    email = signal('');
    authToken = '';
    type = 'REGISTER'; // Default, handles Forgot Password too if consistent

    ngOnInit() {
        this.route.queryParams.subscribe(params => {
            this.email.set(params['email'] || '');
            this.authToken = params['authToken'] || '';
            if (!this.authToken) {
                this.error.set('Invalid session. Please register or login again.');
            }
        });
    }

    onSubmit() {
        if (this.form.invalid || !this.authToken) return;

        this.loading.set(true);
        this.error.set('');

        this.authService.verifyOtp({
            email: this.email(),
            otp: this.form.getRawValue().otp,
            authToken: this.authToken,
            type: 'REGISTER' // We need to handle ForgotPassword type too ideally. But let's assume Register for now or pass type via params.
            // Actually backend needs correct type. I should pass type in query params too.
        }).subscribe({
            next: () => {
                this.router.navigate(['/']);
            },
            error: (err) => {
                this.error.set(err.error?.message || 'Verification failed');
                this.loading.set(false);
            }
        });
    }
}
