import { Component, inject, signal, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RestaurantService } from '../core/services/restaurant.service';
import { Category, MenuItem, Restaurant, Option } from '../core/restaurant/restaurant.types';
import { CartService } from '../core/services/cart.service';
import { RouterLink } from '@angular/router';
import { CustomizeItemDialogComponent } from './components/customize-item-dialog.component';

@Component({
  selector: 'app-restaurant-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, CustomizeItemDialogComponent],
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

  selectedItemForCustomization = signal<MenuItem | null>(null);

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
    return this.menuItems()
      .filter(item => item.categoryId === categoryId && item.available);
  }

  addToCart(item: MenuItem) {
    // If item has options, open customization dialog
    if (item.optionGroups && item.optionGroups.length > 0) {
      this.selectedItemForCustomization.set(item);
      return;
    }

    // Otherwise add directly
    this.processAddToCart(item, [], item.basePrice);
  }

  handleCustomizationComplete(event: { item: MenuItem, selectedOptions: { groupId: string, option: Option }[], totalPrice: number }) {
    this.selectedItemForCustomization.set(null); // Close dialog
    this.processAddToCart(event.item, event.selectedOptions, event.totalPrice);
  }

  processAddToCart(item: MenuItem, options: { groupId: string, option: Option }[], finalPrice: number) {
    const r = this.restaurant();
    if (!r) return;

    this.cartService.addToCart({
      restaurantId: r.id,
      itemId: item.id,
      quantity: 1,
      options: options.map(o => ({
        groupId: o.groupId,
        optionId: o.option.id
      }))
    }).subscribe({
      next: () => {
        // Optional: Toast or subtle animation
        console.log('Added to cart');
      },
      error: (err) => {
        console.error(err);
        alert('Failed to add to cart');
      }
    });
  }
}
