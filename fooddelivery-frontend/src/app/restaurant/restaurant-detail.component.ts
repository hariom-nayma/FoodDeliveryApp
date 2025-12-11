import { Component, inject, signal, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Category, MenuItem, Restaurant, RestaurantService } from '../core/services/restaurant.service';
import { CartService } from '../core/services/cart.service';

@Component({
  selector: 'app-restaurant-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './restaurant-detail.component.html',
  styleUrl: './restaurant-detail.component.css'
})
export class RestaurantDetailComponent implements OnInit {
  private restaurantService = inject(RestaurantService);
  cartService = inject(CartService);

  @Input() id!: string;

  restaurant = signal<Restaurant | null>(null);
  categories = signal<Category[]>([]);
  menuItems = signal<MenuItem[]>([]);

  ngOnInit() {
    if (this.id) {
      this.loadData(this.id);
    }
  }

  loadData(id: string) {
    this.restaurantService.getRestaurant(id).subscribe(data => this.restaurant.set(data));
    this.restaurantService.getCategories(id).subscribe(data => this.categories.set(data));
    this.restaurantService.getMenuItems(id).subscribe(data => this.menuItems.set(data));
  }

  getItemsByCategory(categoryId: string) {
    return this.menuItems().filter(item => item.categoryId === categoryId);
  }

  addToCart(item: MenuItem) {
    const r = this.restaurant();
    if (!r) return;

    // For now, handling simple add without options. 
    // If options needed, we should open a dialog.
    // Assuming simple item for MVP if options property logic is pending.

    this.cartService.addToCart({
      restaurantId: r.id,
      itemId: item.id,
      quantity: 1,
      options: [] // Empty for now, update when option dialog ready
    }).subscribe({
      next: () => {
        // Maybe show specific toast? Global error handler or service might handle generic feedback?
        // Since we use signals in CartService, header auto-updates.
        alert('Item added to cart!'); // Simple feedback for now
      },
      error: (err) => {
        // Basic error handling
        console.error(err);
        alert('Failed to add item to cart. ' + (err.error?.message || ''));
      }
    });
  }
}
