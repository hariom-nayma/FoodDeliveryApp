import { Component, Input, Output, EventEmitter, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MenuItem, OptionGroup, Option } from '../../core/restaurant/restaurant.types';

@Component({
    selector: 'app-customize-item-dialog',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
            <h3>{{ item.name }}</h3>
            <button class="close-btn" (click)="close.emit()">✕</button>
        </div>
        
        <div class="modal-body">
            @for (group of item.optionGroups; track group.id) {
                <div class="option-group">
                    <div class="group-title">
                        <h4>{{ group.name }}</h4>
                        <span class="badge" *ngIf="group.required">Required</span>
                    </div>

                    @if (group.multiSelect) {
                        <!-- Checkboxes for Multi Select -->
                         @for (opt of group.options; track opt.id) {
                            <label class="option-row">
                                <div class="opt-left">
                                    <input type="checkbox" 
                                        [checked]="isSelected(group.id, opt.id)"
                                        (change)="toggleOption(group, opt, $event)">
                                    <span class="opt-name">{{ opt.label }}</span>
                                </div>
                                <span class="opt-price" *ngIf="opt.extraPrice">+ ₹{{ opt.extraPrice }}</span>
                            </label>
                         }
                    } @else {
                        <!-- Radio buttons for Single Select -->
                        @for (opt of group.options; track opt.id) {
                            <label class="option-row">
                                <div class="opt-left">
                                    <input type="radio" 
                                        [name]="group.id" 
                                        [value]="opt.id"
                                        [checked]="isSelected(group.id, opt.id)"
                                        (change)="selectSingleOption(group, opt)">
                                    <span class="opt-name">{{ opt.label }}</span>
                                </div>
                                <span class="opt-price" *ngIf="opt.extraPrice">+ ₹{{ opt.extraPrice }}</span>
                            </label>
                         }
                    }
                </div>
            }
        </div>

        <div class="modal-footer">
            <div class="total-price">
                <span>Total:</span>
                <strong>₹{{ totalPrice() }}</strong>
            </div>
            <button class="add-btn" [disabled]="!isValid()" (click)="onAddToCart()">
                Add to Cart
            </button>
        </div>
      </div>
    </div>
  `,
    styles: [`
    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); display: flex; align-items: flex-end; justify-content: center; z-index: 1100; animation: fadeIn 0.2s; }
    .modal-content { background: white; width: 100%; max-width: 500px; border-radius: 16px 16px 0 0; padding: 1.5rem; max-height: 85vh; display: flex; flex-direction: column; animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
    
    @media(min-width: 768px) {
        .modal-overlay { align-items: center; }
        .modal-content { border-radius: 16px; }
    }

    .modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; border-bottom: 1px solid #eee; padding-bottom: 1rem; }
    .modal-header h3 { margin: 0; font-size: 1.25rem; }
    .close-btn { background: none; border: none; font-size: 1.5rem; cursor: pointer; color: #666; }

    .modal-body { overflow-y: auto; flex: 1; padding-bottom: 1rem; }
    
    .option-group { margin-bottom: 2rem; }
    .group-title { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
    .group-title h4 { margin: 0; font-size: 1rem; font-weight: 600; color: #333; }
    .badge { font-size: 0.7rem; background: #fff3cd; color: #856404; padding: 2px 6px; border-radius: 4px; font-weight: 600; text-transform: uppercase; }

    .option-row { display: flex; justify-content: space-between; align-items: center; padding: 0.8rem 0; border-bottom: 1px solid #f8f8f8; cursor: pointer; }
    .opt-left { display: flex; align-items: center; gap: 10px; }
    .opt-name { font-size: 0.95rem; color: #444; }
    .opt-price { font-size: 0.9rem; color: #666; }

    /* Custom Input Styles */
    input[type="checkbox"], input[type="radio"] { accent-color: #e23744; width: 18px; height: 18px; cursor: pointer; }

    .modal-footer { border-top: 1px solid #eee; padding-top: 1rem; display: flex; justify-content: space-between; align-items: center; }
    .total-price { display: flex; flex-direction: column; }
    .total-price span { font-size: 0.85rem; color: #666; }
    .total-price strong { font-size: 1.25rem; color: #111; }

    .add-btn { background: #e23744; color: white; border: none; padding: 0.8rem 2rem; border-radius: 8px; font-weight: 600; font-size: 1rem; cursor: pointer; transition: background 0.2s; }
    .add-btn:disabled { background: #ccc; cursor: not-allowed; }
    .add-btn:hover:not(:disabled) { background: #d12e3b; }

    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }
  `]
})
export class CustomizeItemDialogComponent implements OnInit {
    @Input() item!: MenuItem;
    @Output() close = new EventEmitter<void>();
    @Output() addToCart = new EventEmitter<{ item: MenuItem, selectedOptions: { groupId: string, option: Option }[], totalPrice: number }>();

    selectedOptions = signal<Map<string, Option[]>>(new Map()); // Map<GroupId, SelectedOptions[]>

    totalPrice = computed(() => {
        let price = this.item.basePrice;
        for (const groupOpts of this.selectedOptions().values()) {
            for (const opt of groupOpts) {
                price += (opt.extraPrice || 0);
            }
        }
        return price;
    });

    isValid = computed(() => {
        // Check required groups
        if (!this.item.optionGroups) return true;

        for (const group of this.item.optionGroups) {
            if (group.required) {
                const selected = this.selectedOptions().get(group.id);
                if (!selected || selected.length === 0) return false;
            }
        }
        return true;
    });

    ngOnInit() {
        // Initialize?
    }

    isSelected(groupId: string, optionId: string): boolean {
        const opts = this.selectedOptions().get(groupId);
        return opts ? opts.some(o => o.id === optionId) : false;
    }

    toggleOption(group: OptionGroup, option: Option, event: any) {
        const isChecked = event.target.checked;
        const currentMap = new Map(this.selectedOptions());
        const currentOpts = currentMap.get(group.id) || [];

        if (isChecked) {
            currentMap.set(group.id, [...currentOpts, option]);
        } else {
            currentMap.set(group.id, currentOpts.filter(o => o.id !== option.id));
        }
        this.selectedOptions.set(currentMap);
    }

    selectSingleOption(group: OptionGroup, option: Option) {
        const currentMap = new Map(this.selectedOptions());
        currentMap.set(group.id, [option]); // Replace existing
        this.selectedOptions.set(currentMap);
    }

    onAddToCart() {
        // Flatten selected options with groupId
        const allSelected: { groupId: string, option: Option }[] = [];
        for (const [groupId, opts] of this.selectedOptions().entries()) {
            for (const opt of opts) {
                allSelected.push({ groupId, option: opt });
            }
        }

        this.addToCart.emit({
            item: this.item,
            selectedOptions: allSelected,
            totalPrice: this.totalPrice()
        });
    }
}
