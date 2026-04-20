import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../core/stores/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none">
      @for (toast of toastService.toasts$ | async; track toast.id) {
        <div
          class="pointer-events-auto rounded-lg px-4 py-3 shadow-lg text-white flex items-start gap-3 text-sm"
          [class]="toastClass(toast)">
          <span class="flex-1">{{ toast.message }}</span>
          <button
            (click)="toastService.dismiss(toast.id)"
            class="flex-shrink-0 opacity-80 hover:opacity-100 text-white font-bold leading-none min-h-0 h-4">
            &times;
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastContainerComponent {
  constructor(readonly toastService: ToastService) {}

  toastClass(toast: Toast): string {
    const base = 'bg-';
    switch (toast.severity) {
      case 'success': return base + 'green-600';
      case 'error': return base + 'red-600';
      case 'warn': return base + 'amber-600';
      default: return base + 'sky-600';
    }
  }
}
