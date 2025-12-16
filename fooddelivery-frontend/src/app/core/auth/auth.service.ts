import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

export interface User {
    id: string;
    name: string;
    email: string;
    phone: string;
    role: string;
    status: string;
    premiumExpiry?: string;
}

interface OtpResponse {
    success: boolean;
    message: string;
    data: {
        message: string;
        authToken: string;
        email: string;
    };
}

interface AuthResponse {
    success: boolean;
    message: string;
    data: {
        accessToken: string;
        refreshToken: string;
        expiresIn: number;
        user: User;
    };
}

@Injectable({
    providedIn: 'root'
})
export class AuthService {
    private http = inject(HttpClient);
    private router = inject(Router);
    private apiUrl = '/api/v1/auth';

    currentUser = signal<User | null>(this.getUserFromStorage());
    isAuthenticated = signal<boolean>(!!this.getUserFromStorage());

    login(credentials: any): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/login`, credentials).pipe(
            tap(response => {
                if (response.success) {
                    this.setSession(response.data);
                }
            })
        );
    }

    register(data: any): Observable<OtpResponse> {
        return this.http.post<OtpResponse>(`${this.apiUrl}/register`, data);
    }

    verifyOtp(data: { email: string; otp: string; authToken: string; type: string }): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/verify-otp`, data).pipe(
            tap(response => {
                if (response.success) {
                    this.setSession(response.data);
                }
            })
        );
    }

    forgotPassword(email: string): Observable<OtpResponse> {
        return this.http.post<OtpResponse>(`${this.apiUrl}/forgot-password?email=${email}`, {});
    }

    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        this.currentUser.set(null);
        this.isAuthenticated.set(false);
        this.router.navigate(['/login']);
    }

    private setSession(data: any) {
        localStorage.setItem('token', data.accessToken);
        if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
        }
        localStorage.setItem('user', JSON.stringify(data.user));
        this.currentUser.set(data.user);
        this.isAuthenticated.set(true);
    }

    private getUserFromStorage(): User | null {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    }
}
