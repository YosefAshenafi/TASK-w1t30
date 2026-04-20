import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

type Severity = 'info' | 'success' | 'warn' | 'error';

@Component({
  selector: 'app-banner',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div [class]="bannerClass()" role="alert">
      <span class="flex-1 text-sm">{{ message }}</span>
      @if (dismissible) {
        <button (click)="dismissed.emit()" class="flex-shrink-0 ml-2 font-bold opacity-70 hover:opacity-100 min-h-0 h-5 leading-none">&times;</button>
      }
    </div>
  `,
})
export class BannerComponent {
  @Input() message = '';
  @Input() severity: Severity = 'info';
  @Input() dismissible = false;
  @Output() dismissed = new EventEmitter<void>();

  bannerClass(): string {
    const base = 'flex items-start px-4 py-3 rounded-lg mb-4';
    const colors: Record<Severity, string> = {
      info: 'bg-sky-50 border border-sky-200 text-sky-800',
      success: 'bg-green-50 border border-green-200 text-green-800',
      warn: 'bg-amber-50 border border-amber-200 text-amber-800',
      error: 'bg-red-50 border border-red-200 text-red-800',
    };
    return [base, colors[this.severity]].join(' ');
  }
}
