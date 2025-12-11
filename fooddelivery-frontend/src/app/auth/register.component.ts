import { Component, inject, signal, effect } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import zxcvbn from 'zxcvbn';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css'
})
export class RegisterComponent {
  private fb = inject(NonNullableFormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  form = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  loading = signal(false);
  error = signal('');
  passwordError = signal('');

  constructor() {
      // Validate password on change
      this.form.valueChanges.subscribe(() => {
          this.validatePassword();
      });
  }

  validatePassword(): boolean {
      const { name, email, phone, password } = this.form.getRawValue();
      if (!password) {
          this.passwordError.set('');
          return false;
      }
      if (password.length < 8) {
          this.passwordError.set('Password must be at least 8 characters');
          return false;
      }

      // Personal info check
      const personalInfo = [name, email?.split('@')[0], phone].filter(Boolean);
      const lowerPass = password.toLowerCase();
      for (const info of personalInfo) {
          if (info && info.length > 2 && lowerPass.includes(info.toLowerCase())) {
              this.passwordError.set(`Password cannot contain your personal info (${info})`);
              return false;
          }
      }

      const result = zxcvbn(password, personalInfo);
      if (result.score < 2) {
          this.passwordError.set('Password is too weak. Add more unique words.');
          return false;
      }

      this.passwordError.set('');
      return true;
  }

  onSubmit() {
    if (this.form.invalid) return;
    if (!this.validatePassword()) return; // Double check

    this.loading.set(true);
    this.error.set('');

    this.authService.register(this.form.getRawValue()).subscribe({
      next: (response) => {
        if (response.success) {
          this.router.navigate(['/verify-otp'], {
            queryParams: {
              email: response.data.email,
              authToken: response.data.authToken
            }
          });
        }
      },
      error: (err) => {
        this.error.set(err.error?.message || 'Registration failed');
        this.loading.set(false);
      }
    });
  }
}
