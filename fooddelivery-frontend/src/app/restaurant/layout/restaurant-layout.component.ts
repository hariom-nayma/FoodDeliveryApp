import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
    selector: 'app-restaurant-layout',
    standalone: true,
    imports: [RouterOutlet, RouterLink, RouterLinkActive],
    template: `
    <div class="layout-container">
      <aside class="sidebar">
        <div class="logo">
           <h2>Manage</h2>
        </div>
        <nav>
          <a [routerLink]="['/restaurant/manage', restaurantId, 'dashboard']" routerLinkActive="active" class="nav-item">
            📊 Dashboard
          </a>
          <a [routerLink]="['/restaurant/manage', restaurantId, 'menu']" routerLinkActive="active" class="nav-item">
            🍽️ Menu
          </a>
          <a [routerLink]="['/restaurant/manage', restaurantId, 'orders']" routerLinkActive="active" class="nav-item">
             📦 Orders
          </a>
        </nav>
        
        <div class="logout-section">
            <button (click)="logout()">Logout</button>
        </div>
      </aside>
      <main class="content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
    styles: [`
    .layout-container { display: flex; min-height: 100vh; }
    .sidebar { width: 250px; background: #fff; border-right: 1px solid #eee; display: flex; flex-direction: column; position: fixed; height: 100vh; }
    .logo { padding: 1.5rem; border-bottom: 1px solid #eee; }
    nav { flex: 1; padding: 1rem; }
    .nav-item { display: block; padding: 0.75rem 1rem; color: #666; text-decoration: none; border-radius: 6px; margin-bottom: 0.5rem; transition: all 0.2s; }
    .nav-item:hover { background: #f8f9fa; color: var(--primary-color); }
    .nav-item.active { background: #fff0f3; color: var(--primary-color); font-weight: 500; }
    .nav-item.disabled { opacity: 0.5; cursor: not-allowed; }
    .content { flex: 1; margin-left: 250px; padding: 2rem; background: #fafafa; }
    .logout-section { padding: 1rem; border-top: 1px solid #eee; }
    button { width: 100%; padding: 0.5rem; border: 1px solid #ddd; background: white; border-radius: 4px; cursor: pointer; }
    button:hover { background: #f5f5f5; }
  `]
})
export class RestaurantLayoutComponent {
    private route = inject(ActivatedRoute);
    private auth = inject(AuthService);

    restaurantId = '';

    constructor() {
        // Updated for /restaurant/manage/:id
        const parts = window.location.pathname.split('/');
        // URL: /restaurant/manage/123/dashboard
        const idx = parts.indexOf('manage');
        if (idx !== -1 && parts[idx + 1]) {
            this.restaurantId = parts[idx + 1];
        }
    }

    logout() {
        this.auth.logout();
    }
}
