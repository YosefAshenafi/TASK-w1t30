import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (open) {
      <div
        class="fixed inset-0 z-40 bg-black/40 flex items-center justify-center p-4"
        (click)="onBackdrop($event)">
        <div
          role="dialog"
          [attr.aria-label]="title"
          class="bg-[var(--color-surface)] rounded-2xl shadow-xl w-full max-w-md flex flex-col max-h-[90vh]">
          <div class="flex items-center justify-between px-6 py-4 border-b border-[var(--color-border)]">
            <h2 class="font-semibold text-base">{{ title }}</h2>
            <button
              (click)="closed.emit()"
              class="text-[var(--color-text-muted)] hover:text-[var(--color-text)] min-h-0 h-8 w-8 flex items-center justify-center rounded">
              &times;
            </button>
          </div>
          <div class="flex-1 overflow-y-auto px-6 py-4">
            <ng-content />
          </div>
          @if (hasFooter) {
            <div class="px-6 py-4 border-t border-[var(--color-border)] flex justify-end gap-2">
              <ng-content select="[slot=footer]" />
            </div>
          }
        </div>
      </div>
    }
  `,
})
export class DialogComponent {
  @Input() open = false;
  @Input() title = '';
  @Input() hasFooter = false;
  @Input() closeOnBackdrop = true;
  @Output() closed = new EventEmitter<void>();

  onBackdrop(e: MouseEvent): void {
    if (this.closeOnBackdrop && e.target === e.currentTarget) {
      this.closed.emit();
    }
  }
}
