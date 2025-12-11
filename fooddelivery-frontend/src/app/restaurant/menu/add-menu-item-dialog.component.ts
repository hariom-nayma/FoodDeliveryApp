import { Component, inject, signal, output, input } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators, FormArray, FormGroup } from '@angular/forms';
import { Category } from './menu.service';

@Component({
    selector: 'app-add-menu-item-dialog',
    standalone: true,
    imports: [ReactiveFormsModule],
    template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <h3>Add Menu Item</h3>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          
          <div class="form-row">
              <div class="form-group half">
                <label>Name</label>
                <input type="text" formControlName="name" placeholder="Item name">
              </div>
              <div class="form-group half">
                <label>Price (₹)</label>
                <input type="number" formControlName="basePrice">
              </div>
          </div>

          <div class="form-group">
            <label>Description</label>
            <textarea formControlName="description" rows="2"></textarea>
          </div>

           <div class="form-row">
              <div class="form-group half">
                <label>Category</label>
                <select formControlName="categoryId">
                    <option value="">Select Category</option>
                    @for (cat of categories(); track cat.id) {
                        <option [value]="cat.id">{{ cat.name }}</option>
                    }
                </select>
              </div>
              <div class="form-group half">
                <label>Type</label>
                <select formControlName="foodType">
                    <option value="VEG">Veg</option>
                    <option value="NON_VEG">Non-Veg</option>
                    <option value="VEGAN">Vegan</option>
                </select>
              </div>
          </div>

          <div class="form-group">
            <label>Image</label>
            <input type="file" (change)="onFileSelected($event)" accept="image/*">
            @if (imagePreview()) {
                <img [src]="imagePreview()" class="preview-img">
            }
          </div>

          <div class="form-group checkbox">
             <label>
                <input type="checkbox" formControlName="available"> Available
             </label>
          </div>
          
          <div class="actions">
            <button type="button" class="btn-secondary" (click)="close.emit()">Cancel</button>
            <button type="submit" class="btn-primary" [disabled]="loading()">Save Item</button>
          </div>
        </form>
      </div>
    </div>
  `,
    styles: [`
    .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
    .modal-content { background: white; padding: 2rem; border-radius: 8px; width: 100%; max-width: 600px; max-height: 90vh; overflow-y: auto; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
    h3 { margin-top: 0; margin-bottom: 1.5rem; }
    .form-group { margin-bottom: 1rem; }
    .form-row { display: flex; gap: 1rem; }
    .form-group.half { flex: 1; }
    label { display: block; margin-bottom: 0.5rem; font-weight: 500; font-size: 0.9rem; }
    input[type="text"], input[type="number"], select, textarea, input[type="file"] { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
    .checkbox label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
    .preview-img { width: 100px; height: 100px; object-fit: cover; margin-top: 0.5rem; border-radius: 4px; }
    .actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem; }
    button { padding: 0.75rem 1.5rem; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; }
    .btn-primary { background: var(--primary-color); color: white; }
    .btn-secondary { background: #eee; color: #333; }
    .btn-primary:disabled { opacity: 0.7; cursor: not-allowed; }
  `]
})
export class AddMenuItemDialogComponent {
    private fb = inject(NonNullableFormBuilder);

    categories = input<Category[]>([]);
    close = output<void>();
    save = output<{ itemData: any, imageFile?: File }>();
    loading = signal(false);
    imagePreview = signal<string | null>(null);
    selectedFile: File | undefined;

    form = this.fb.group({
        name: ['', Validators.required],
        description: [''],
        basePrice: [0, [Validators.required, Validators.min(0)]],
        categoryId: ['', Validators.required],
        foodType: ['VEG', Validators.required],
        available: [true]
    });

    onFileSelected(event: any) {
        const file = event.target.files[0];
        if (file) {
            this.selectedFile = file;
            const reader = new FileReader();
            reader.onload = e => this.imagePreview.set(e.target?.result as string);
            reader.readAsDataURL(file);
        }
    }

    onSubmit() {
        if (this.form.valid) {
            this.loading.set(true);
            // Construct logic request
            const itemData = {
                ...this.form.getRawValue(),
                tags: [], // Placeholder
                optionGroups: [] // Placeholder for now, later we can add nested form
            };

            this.save.emit({ itemData, imageFile: this.selectedFile });
        }
    }
}
