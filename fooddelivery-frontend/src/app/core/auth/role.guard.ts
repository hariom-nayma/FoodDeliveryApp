import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const requiredRole = route.data['expectedRole'] as string;
    const requiredRoles = route.data['roles'] as Array<string>;

    const user = authService.currentUser();

    if (!user) {
        return router.createUrlTree(['/login']);
    }

    if (requiredRole && user.role !== requiredRole) {
        return router.createUrlTree(['/']);
    }

    if (requiredRoles && !requiredRoles.includes(user.role)) {
        return router.createUrlTree(['/']);
    }

    return true;
};
