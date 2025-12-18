import { Component, inject, effect } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './core/header/header.component';
import { FooterComponent } from './core/footer/footer.component';
import { ActiveOrderOverlayComponent } from './components/order/active-order-overlay.component';
import { AuthService } from './core/auth/auth.service';
import { SocketService } from './core/services/socket.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, ActiveOrderOverlayComponent],
  template: `
    <app-header></app-header>
    <main style="min-height: 80vh;">
      <router-outlet></router-outlet>
    </main>
    <app-active-order-overlay></app-active-order-overlay>
    <app-footer></app-footer>
  `,
  styles: []
})
export class AppComponent {
  title = 'fooddelivery-frontend';

  private authService = inject(AuthService);
  private socketService = inject(SocketService);

  constructor() {
    effect(() => {
      const user = this.authService.currentUser();
      if (user) {
        this.socketService.joinUserRoom(user.id);
      }
    });
  }
}
