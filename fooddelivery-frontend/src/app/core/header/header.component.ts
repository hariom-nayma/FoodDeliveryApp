import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../auth/auth.service';
import { CartService } from '../services/cart.service';
import { AddAddressComponent } from '../../shared/components/add-address/add-address.component';
import { AsyncPipe, CommonModule } from '@angular/common';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, CommonModule, AsyncPipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent {
  authService = inject(AuthService);
  dialog = inject(MatDialog);
  cartService = inject(CartService);
  showMenu = false;

  toggleMenu() {
    this.showMenu = !this.showMenu;
  }

  isCustomer() {
    const role = this.authService.currentUser()?.role;
    return !role || role === 'ROLE_CUSTOMER';
  }

  isOwner() {
    return this.authService.currentUser()?.role === 'ROLE_RESTAURANT_OWNER';
  }

  isDeliveryPartner() {
    return this.authService.currentUser()?.role === 'ROLE_DELIVERY_PARTNER';
  }

  openAddAddress() {
    this.dialog.open(AddAddressComponent, {
      width: '600px',
      maxWidth: '95vw'
    });
  }
}
