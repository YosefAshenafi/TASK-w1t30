import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface Tab {
  id: string;
  label: string;
  badge?: number;
}

@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="border-b border-[var(--color-border)] flex gap-1 overflow-x-auto">
      @for (tab of tabs; track tab.id) {
        <button
          (click)="activeChange.emit(tab.id)"
          [class]="tabClass(tab.id)"
          class="px-4 py-2 text-sm font-medium whitespace-nowrap min-h-[48px] border-b-2 -mb-px transition-colors">
          {{ tab.label }}
          @if (tab.badge !== undefined) {
            <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-full bg-[var(--color-brand-100)] text-[var(--color-brand-700)]">
              {{ tab.badge }}
            </span>
          }
        </button>
      }
    </div>
  `,
})
export class TabsComponent {
  @Input() tabs: Tab[] = [];
  @Input() active = '';
  @Output() activeChange = new EventEmitter<string>();

  tabClass(id: string): string {
    return id === this.active
      ? 'border-[var(--color-brand-600)] text-[var(--color-brand-600)]'
      : 'border-transparent text-[var(--color-text-muted)] hover:text-[var(--color-text)]';
  }
}
