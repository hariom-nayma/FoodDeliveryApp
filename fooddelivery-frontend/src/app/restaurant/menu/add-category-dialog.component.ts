import { Component, inject, signal, output } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
    selector: 'app-add-category-dialog',
    standalone: true,
    imports: [ReactiveFormsModule],
    template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <h3>Add Category</h3>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label>Name</label>
            <input type="text" formControlName="name" placeholder="e.g. Starters">
          </div>
          <div class="form-group">
            <label>Description</label>
            <input type="text" formControlName="description" placeholder="Optional description">
          </div>
          <div class="form-group">
            <label>Sort Order</label>
            <input type="number" formControlName="sortOrder">
          </div>
          
          <div class="actions">
            <button type="button" class="btn-secondary" (click)="close.emit()">Cancel</button>
            <button type="submit" class="btn-primary" [disabled]="loading()">Save</button>
          </div>
        </form>
      </div>
    </div>
  `,
    styles: [`
    .modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); display: flex; justify-content: center; align-items: center; z-index: 1000; }
    .modal-content { background: white; padding: 2rem; border-radius: 8px; width: 100%; max-width: 400px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
    h3 { margin-top: 0; margin-bottom: 1.5rem; }
    .form-group { margin-bottom: 1rem; }
    label { display: block; margin-bottom: 0.5rem; font-weight: 500; font-size: 0.9rem; }
    input { width: 100%; padding: 0.75rem; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
    .actions { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 1.5rem; }
    button { padding: 0.75rem 1.5rem; border: none; border-radius: 4px; cursor: pointer; font-weight: 500; }
    .btn-primary { background: var(--primary-color); color: white; }
    .btn-secondary { background: #eee; color: #333; }
    .btn-primary:disabled { opacity: 0.7; cursor: not-allowed; }
  `]
})
export class AddCategoryDialogComponent {
    private fb = inject(NonNullableFormBuilder);

    close = output<void>();
    save = output<any>();
    loading = signal(false);

    form = this.fb.group({
        name: ['', Validators.required],
        description: [''],
        sortOrder: [0]
    });

    onSubmit() {
        if (this.form.valid) {
            this.loading.set(true);
            this.save.emit(this.form.getRawValue());
        }
    }
}
