import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogService } from '../../services/dialog.service';

@Component({
    selector: 'app-global-dialog',
    standalone: true,
    imports: [CommonModule],
    template: `
    @if (dialog()) {
        <div class="dialog-overlay" (click)="close()">
            <div class="dialog-card" [class]="dialog()?.type" (click)="$event.stopPropagation()">
                <div class="icon-wrapper">
                    @if (dialog()?.type === 'success') { <span>✅</span> }
                    @if (dialog()?.type === 'error') { <span>❌</span> }
                    @if (dialog()?.type === 'warning' || dialog()?.type === 'confirm') { <span>⚠️</span> }
                    @if (dialog()?.type === 'info') { <span>ℹ️</span> }
                </div>
                
                <h3 *ngIf="dialog()?.title">{{ dialog()?.title }}</h3>
                <p>{{ dialog()?.message }}</p>

                <div class="actions">
                    @if (dialog()?.type === 'confirm') {
                        <button class="btn-cancel" (click)="cancel()">{{ dialog()?.cancelText || 'Cancel' }}</button>
                        <button class="btn-confirm" (click)="confirm()">{{ dialog()?.confirmText || 'Confirm' }}</button>
                    } @else {
                        <button class="btn-ok" (click)="close()">OK</button>
                    }
                </div>
            </div>
        </div>
    }
  `,
    styles: [`
    .dialog-overlay {
        position: fixed;
        inset: 0;
        background: rgba(0, 0, 0, 0.4);
        backdrop-filter: blur(5px);
        z-index: 2000;
        display: flex;
        align-items: center;
        justify-content: center;
        animation: fadeIn 0.2s ease-out;
    }

    .dialog-card {
        background: rgba(255, 255, 255, 0.95);
        min-width: 320px;
        max-width: 90%;
        padding: 2rem;
        border-radius: 24px;
        box-shadow: 0 20px 60px rgba(0,0,0,0.2), 0 0 0 1px rgba(255,255,255,0.5) inset;
        text-align: center;
        animation: scaleIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
    }

    .icon-wrapper {
        font-size: 3rem;
        margin-bottom: 0.5rem;
        animation: bounceIn 0.5s cubic-bezier(0.16, 1, 0.3, 1);
    }

    h3 {
        margin: 0;
        font-size: 1.5rem;
        color: #111;
        font-weight: 700;
    }

    p {
        margin: 0;
        color: #555;
        font-size: 1rem;
        line-height: 1.5;
    }

    .actions {
        display: flex;
        gap: 1rem;
        margin-top: 1rem;
        width: 100%;
        justify-content: center;
    }

    button {
        padding: 0.8rem 2rem;
        border: none;
        border-radius: 12px;
        font-weight: 600;
        cursor: pointer;
        font-size: 1rem;
        transition: transform 0.1s;
    }

    button:active {
        transform: scale(0.96);
    }

    .btn-ok { background: #111; color: white; width: 100%; }
    .btn-confirm { background: #e23744; color: white; flex: 1; }
    .btn-cancel { background: #f0f0f0; color: #333; flex: 1; }

    /* Type Specific Styles */
    .dialog-card.success .btn-ok { background: #00875a; }
    .dialog-card.error .btn-ok { background: #de350b; }
    
    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
    @keyframes scaleIn { from { transform: scale(0.9) translateY(20px); opacity: 0; } to { transform: scale(1) translateY(0); opacity: 1; } }
    @keyframes bounceIn { 0% { transform: scale(0); } 50% { transform: scale(1.2); } 100% { transform: scale(1); } }
  `]
})
export class GlobalDialogComponent {
    dialogService = inject(DialogService);
    dialog = this.dialogService.activeDialog;

    close() {
        this.dialogService.close();
    }

    confirm() {
        this.dialog()?.onConfirm?.();
    }

    cancel() {
        this.dialog()?.onCancel?.();
    }
}
