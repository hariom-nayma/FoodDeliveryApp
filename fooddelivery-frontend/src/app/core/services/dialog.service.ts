import { Injectable, signal } from '@angular/core';

export type DialogType = 'success' | 'error' | 'info' | 'warning' | 'confirm';

export interface DialogConfig {
    title?: string;
    message: string;
    type: DialogType;
    confirmText?: string;
    cancelText?: string;
    onConfirm?: () => void;
    onCancel?: () => void;
}

@Injectable({
    providedIn: 'root'
})
export class DialogService {
    activeDialog = signal<DialogConfig | null>(null);

    open(config: DialogConfig) {
        this.activeDialog.set(config);
    }

    close() {
        this.activeDialog.set(null);
    }

    alert(message: string, title?: string, type: DialogType = 'info') {
        this.open({
            message,
            title,
            type
        });
    }

    confirm(message: string, title = 'Are you sure?', confirmText = 'Yes', cancelText = 'No'): Promise<boolean> {
        return new Promise((resolve) => {
            this.open({
                message,
                title,
                type: 'confirm',
                confirmText,
                cancelText,
                onConfirm: () => {
                    this.close();
                    resolve(true);
                },
                onCancel: () => {
                    this.close();
                    resolve(false);
                }
            });
        });
    }
}
