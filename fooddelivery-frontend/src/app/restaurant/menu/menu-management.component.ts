import { Component, inject, signal, OnInit } from '@angular/core';
import { MenuService, Category, MenuItem } from './menu.service';
import { ActivatedRoute } from '@angular/router';
import { AddCategoryDialogComponent } from './add-category-dialog.component';
import { AddMenuItemDialogComponent } from './add-menu-item-dialog.component';

@Component({
  selector: 'app-menu-management',
  standalone: true,
  imports: [AddCategoryDialogComponent, AddMenuItemDialogComponent],
  template: `
    <div class="container">
        <header>
            <h2>Menu Management</h2>
            <div class="tabs">
                <button [class.active]="activeTab() === 'categories'" (click)="activeTab.set('categories')">Categories</button>
                <button [class.active]="activeTab() === 'items'" (click)="activeTab.set('items')">Menu Items</button>
            </div>
        </header>

        @if (activeTab() === 'categories') {
            <div class="section">
                <div class="actions">
                    <h3>All Categories</h3>
                    <button class="btn-primary" (click)="showCategoryDialog.set(true)">Add Category</button>
                </div>
                <!-- List -->
                <div class="list">
                    @for (cat of categories(); track cat.id) {
                        <div class="item-card">
                            <span>{{ cat.name }}</span>
                            <span class="badge" [class.inactive]="!cat.active">{{ cat.active ? 'Active' : 'Inactive' }}</span>
                        </div>
                    }
                    @if (categories().length === 0) {
                        <p class="empty-state">No categories found. Add one to start!</p>
                    }
                </div>
            </div>
        }

        @if (activeTab() === 'items') {
            <div class="section">
                <div class="actions">
                    <h3>All Items</h3>
                    <button class="btn-primary" (click)="showItemDialog.set(true)">Add Item</button>
                </div>
                <div class="list">
                     @for (item of items(); track item.id) {
                        <div class="item-card">
                            <img [src]="item.imageUrl || 'assets/placeholder.png'" width="50" height="50">
                            <div class="details">
                                <h4>{{ item.name }}</h4>
                                <p>{{ item.description }}</p>
                                <span class="price">₹{{ item.basePrice }}</span>
                            </div>
                        </div>
                    }
                    @if (items().length === 0) {
                        <p class="empty-state">No menu items found.</p>
                    }
                </div>
            </div>
        }

        <!-- Dialogs -->
        @if (showCategoryDialog()) {
            <app-add-category-dialog 
                (close)="showCategoryDialog.set(false)"
                (save)="saveCategory($event)"
            ></app-add-category-dialog>
        }

        @if (showItemDialog()) {
            <app-add-menu-item-dialog 
                [categories]="categories()"
                [item]="selectedItem()"
                (close)="showItemDialog.set(false); selectedItem.set(null)"
                (save)="saveItem($event)"
            ></app-add-menu-item-dialog>
        }
    </div>
  `,
  styles: [`
    .container { padding: 1rem; }
    header { margin-bottom: 2rem; }
    .tabs { display: flex; gap: 1rem; border-bottom: 1px solid #ddd; margin-top: 1rem; }
    .tabs button { background: none; border: none; padding: 0.75rem 1.5rem; cursor: pointer; font-size: 1rem; border-bottom: 2px solid transparent; }
    .tabs button.active { border-bottom-color: var(--primary-color); color: var(--primary-color); font-weight: 500; }
    .actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .btn-primary { background: var(--primary-color); color: white; border: none; padding: 0.5rem 1rem; border-radius: 4px; cursor: pointer; }
    .list { display: flex; flex-direction: column; gap: 1rem; }
    .item-card { background: white; padding: 1rem; border-radius: 8px; border: 1px solid #eee; display: flex; align-items: center; gap: 1rem; }
    .badge { padding: 0.25rem 0.5rem; background: #e6f4ea; color: #1e8e3e; border-radius: 12px; font-size: 0.8rem; }
    .badge.inactive { background: #fce8e6; color: #d93025; }
    .price { font-weight: bold; }
    .details { flex: 1; }
    .details h4 { margin: 0 0 0.25rem; }
    .details p { margin: 0 0 0.5rem; font-size: 0.9rem; color: #666; }
    .empty-state { text-align: center; color: #888; padding: 2rem; }
    img { object-fit: cover; border-radius: 4px; }
  `]
})
export class MenuManagementComponent implements OnInit {
    private menuService = inject(MenuService);
    private route = inject(ActivatedRoute);

    activeTab = signal('categories');
    categories = signal<Category[]>([]);
    items = signal<MenuItem[]>([]);
    restaurantId = '';

    showCategoryDialog = signal(false);
    showItemDialog = signal(false);
    selectedItem = signal<any>(null);

    ngOnInit() {
        const parts = window.location.pathname.split('/');
        const idx = parts.indexOf('manage');
        if (idx !== -1 && parts[idx+1]) {
            this.restaurantId = parts[idx+1];
            this.loadData();
        }
    }

    loadData() {
        this.menuService.getCategories(this.restaurantId).subscribe(res => {
            if (res.success) this.categories.set(res.data);
        });
        this.menuService.getMenuItems(this.restaurantId).subscribe(res => {
            if (res.success) this.items.set(res.data);
        });
    }

    saveCategory(data: any) {
        this.menuService.createCategory(this.restaurantId, data).subscribe({
            next: (res) => {
                if (res.success) {
                    this.loadData();
                    this.showCategoryDialog.set(false);
                }
            },
            error: (err) => alert('Failed to create category')
        });
    }

    editItem(item: any) {
        this.selectedItem.set(item);
        this.showItemDialog.set(true);
    }

    saveItem(event: { itemData: any, imageFile?: File }) {
        const obs = this.selectedItem() 
            ? this.menuService.updateMenuItem(this.restaurantId, this.selectedItem().id, event.itemData, event.imageFile)
            : this.menuService.createMenuItem(this.restaurantId, event.itemData, event.imageFile);

        obs.subscribe({
            next: (res) => {
                if (res.success) {
                    this.loadData();
                    this.showItemDialog.set(false);
                    this.selectedItem.set(null);
                }
            },
            error: (err) => alert('Failed to save item: ' + (err.error?.message || err.message))
        });
    }
}
