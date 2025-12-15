import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { roleGuard } from './core/auth/role.guard';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';
import { HomeComponent } from './home/home.component';
import { RestaurantDetailComponent } from './restaurant/restaurant-detail.component';

export const routes: Routes = [
    { path: '', component: HomeComponent },
    { path: 'login', component: LoginComponent },
    { path: 'register', component: RegisterComponent },
    { path: 'verify-otp', loadComponent: () => import('./auth/verify-otp.component').then(m => m.VerifyOtpComponent) },
    { path: 'my-orders', loadComponent: () => import('./customer/orders/my-orders.component').then(m => m.MyOrdersComponent), canActivate: [authGuard] }, // Optional: Add roleGuard for 'ROLE_CUSTOMER' if strictest needed

    {
        path: 'restaurant/register',
        loadComponent: () => import('./restaurant/register/restaurant-register.component').then(m => m.RestaurantRegisterComponent),
        canActivate: [authGuard]
    },
    {
        path: 'restaurant/pending',
        loadComponent: () => import('./restaurant/pending/pending-verification.component').then(m => m.PendingVerificationComponent),
        canActivate: [authGuard],
    },
    {
        path: 'restaurant/:id/documents',
        loadComponent: () => import('./restaurant/documents/document-upload.component').then(m => m.DocumentUploadComponent),
        canActivate: [authGuard]
    },
    {
        path: 'partner/home',
        loadComponent: () => import('./components/delivery-partner-home/delivery-partner-home').then(m => m.DeliveryPartnerHome),
        canActivate: [authGuard, roleGuard],
        data: { expectedRole: 'ROLE_DELIVERY_PARTNER' }
    },
    { path: 'cart', loadComponent: () => import('./components/cart/cart.component').then(m => m.CartComponent), canActivate: [authGuard] },
    { path: 'join-delivery', loadComponent: () => import('./components/delivery-partner-signup/delivery-partner-signup').then(m => m.DeliveryPartnerSignup), canActivate: [authGuard] }, // Maybe public or auth needed

    // Owner Layout Routes
    {
        path: 'restaurant/manage/:id',
        loadComponent: () => import('./restaurant/layout/restaurant-layout.component').then(m => m.RestaurantLayoutComponent),
        canActivate: [authGuard, roleGuard],
        data: { expectedRole: 'ROLE_RESTAURANT_OWNER' },
        children: [
            { path: 'dashboard', loadComponent: () => import('./restaurant/dashboard/owner-dashboard.component').then(m => m.OwnerDashboardComponent) },
            { path: 'menu', loadComponent: () => import('./restaurant/menu/menu-management.component').then(m => m.MenuManagementComponent) },
            { path: 'orders', loadComponent: () => import('./restaurant/orders/restaurant-orders.component').then(m => m.RestaurantOrdersComponent) },
            { path: 'settings', loadComponent: () => import('./restaurant/settings/restaurant-settings.component').then(m => m.RestaurantSettingsComponent) },
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
    },

    { path: 'restaurant/:id', component: RestaurantDetailComponent },
    { path: '**', redirectTo: '' }
];
