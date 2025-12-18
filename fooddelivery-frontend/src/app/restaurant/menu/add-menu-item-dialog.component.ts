import { Component, inject, signal, output, Input } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators, FormArray, FormGroup } from '@angular/forms';
import { Category } from './menu.service';

@Component({
  selector: 'app-add-menu-item-dialog',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <h3>{{ isEditMode ? 'Edit' : 'Add' }} Menu Item</h3>
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
                    @for (cat of categories; track cat.id) {
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
          
          <!-- Option Groups Section -->
          <div class="option-groups-section">
            <div class="section-header">
                <h4>Variations & Add-ons</h4>
                <button type="button" class="btn-small" (click)="addOptionGroup()">+ Add Group</button>
            </div>
            
            <div formArrayName="optionGroups">
                @for (group of optionGroups.controls; track $index) {
                    <div [formGroupName]="$index" class="group-card">
                        <div class="group-header">
                            <input type="text" formControlName="name" placeholder="Group Name (e.g. Size, Toppings)">
                            <button type="button" class="btn-icon" (click)="removeOptionGroup($index)">🗑️</button>
                        </div>
                        <div class="form-row small">
                            <label><input type="checkbox" formControlName="multiSelect"> Multi-select</label>
                            <label><input type="checkbox" formControlName="required"> Required</label>
                        </div>
                        
                        <!-- Options within Group -->
                        <div formArrayName="options" class="options-list">
                            @for (opt of getOptions(group).controls; track $index) {
                                <div [formGroupName]="$index" class="option-row">
                                    <input type="text" formControlName="label" placeholder="Option Name">
                                    <input type="number" formControlName="extraPrice" placeholder="Price">
                                    <button type="button" class="btn-icon" (click)="removeOption($index, group)">✕</button>
                                </div>
                            }
                            <button type="button" class="btn-link" (click)="addOption(group)">+ Add Option</button>
                        </div>
                    </div>
                }
            </div>
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
    .modal-content { background: white; padding: 2rem; border-radius: 8px; width: 100%; max-width: 700px; max-height: 90vh; overflow-y: auto; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
    h3 { margin-top: 0; margin-bottom: 1.5rem; }
    h4 { margin: 0; }
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
    
    /* Option Groups */
    .option-groups-section { margin-top: 1.5rem; border-top: 1px solid #eee; padding-top: 1rem; }
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .btn-small { background: #e0e0e0; padding: 0.4rem 0.8rem; font-size: 0.85rem; }
    .group-card { background: #f9f9f9; padding: 1rem; border-radius: 6px; border: 1px solid #eee; margin-bottom: 1rem; }
    .group-header { display: flex; gap: 10px; margin-bottom: 0.5rem; }
    .form-row.small { font-size: 0.85rem; display: flex; gap: 1rem; margin-bottom: 0.5rem; }
    .options-list { margin-left: 1rem; border-left: 2px solid #eee; padding-left: 1rem; }
    .option-row { display: flex; gap: 10px; margin-bottom: 0.5rem; align-items: center; }
    .option-row input { padding: 0.4rem; font-size: 0.9rem; }
    .btn-icon { background: none; padding: 0 5px; color: #888; font-size: 1.1rem; }
    .btn-icon:hover { color: red; }
    .btn-link { background: none; color: var(--primary-color); padding: 0; font-size: 0.85rem; text-decoration: underline; margin-top: 5px; }
  `]
})
export class AddMenuItemDialogComponent {
  private fb = inject(NonNullableFormBuilder);

  @Input() categories: Category[] = [];
  @Input() item: any = null; // Context for editing
  close = output<void>();
  save = output<{ itemData: any, imageFile?: File }>();
  loading = signal(false);
  imagePreview = signal<string | null>(null);
  selectedFile: File | undefined;
  isEditMode = false;

  constructor() { } // Explicitly empty or remove if not needed, but keep it clean.

  ngOnInit() {
    const itemData = this.item;
    if (itemData) {
      this.isEditMode = true;
      this.form.patchValue({
        name: itemData.name,
        description: itemData.description,
        basePrice: itemData.basePrice,
        categoryId: itemData.categoryId,
        foodType: itemData.foodType,
        available: itemData.available
      });
      this.imagePreview.set(itemData.imageUrl);

      // Reconstruct Option Groups
      if (itemData.optionGroups) {
        itemData.optionGroups.forEach((g: any) => {
          const group = this.fb.group({
            name: [g.name, Validators.required],
            multiSelect: [g.multiSelect],
            required: [g.required],
            options: this.fb.array([])
          });

          if (g.options) {
            g.options.forEach((o: any) => {
              (group.get('options') as FormArray).push(this.fb.group({
                label: [o.label || o.name, Validators.required],
                extraPrice: [o.extraPrice || o.additionalPrice || 0, Validators.min(0)],
                available: [true]
              }));
            });
          }

          this.optionGroups.push(group);
        });
      }
    }
  }

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    basePrice: [0, [Validators.required, Validators.min(0)]],
    categoryId: ['', Validators.required],
    foodType: ['VEG', Validators.required],
    available: [true],
    optionGroups: this.fb.array([])
  });

  get optionGroups() {
    return this.form.get('optionGroups') as FormArray;
  }

  getOptions(group: any) {
    return group.get('options') as FormArray;
  }

  addOptionGroup() {
    const group = this.fb.group({
      name: ['', Validators.required],
      multiSelect: [false],
      required: [false],
      options: this.fb.array([])
    });
    // Add one empty option by default
    (group.get('options') as FormArray).push(this.createOption());
    this.optionGroups.push(group);
  }

  removeOptionGroup(index: number) {
    this.optionGroups.removeAt(index);
  }

  addOption(group: any) {
    const options = group.get('options') as FormArray;
    options.push(this.createOption());
  }

  removeOption(index: number, group: any) {
    const options = group.get('options') as FormArray;
    options.removeAt(index);
  }

  createOption() {
    return this.fb.group({
      label: ['', Validators.required],
      extraPrice: [0, Validators.min(0)],
      available: [true]
    });
  }

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
      const itemData = this.form.getRawValue();
      this.save.emit({ itemData, imageFile: this.selectedFile });
    } else {
      this.form.markAllAsTouched();
    }
  }
}
