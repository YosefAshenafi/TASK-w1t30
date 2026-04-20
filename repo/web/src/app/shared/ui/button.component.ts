import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost';
type Size = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled || loading"
      [class]="classes()">
      @if (loading) { <span class="inline-block w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin mr-2"></span> }
      <ng-content />
    </button>
  `,
})
export class ButtonComponent {
  @Input() variant: Variant = 'primary';
  @Input() size: Size = 'md';
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() disabled = false;
  @Input() loading = false;

  classes(): string {
    const base = 'inline-flex items-center justify-center rounded-lg font-medium transition-colors disabled:opacity-50 focus:outline-none focus:ring-2 focus:ring-offset-2';
    const sizes: Record<Size, string> = {
      sm: 'px-3 text-sm min-h-[36px]',
      md: 'px-4 text-sm min-h-[48px]',
      lg: 'px-6 text-base min-h-[52px]',
    };
    const variants: Record<Variant, string> = {
      primary: 'bg-[var(--color-brand-600)] text-white hover:bg-[var(--color-brand-700)] focus:ring-[var(--color-brand-500)]',
      secondary: 'bg-[var(--color-surface-overlay)] text-[var(--color-text)] hover:bg-[var(--color-border)] focus:ring-[var(--color-border-focus)]',
      danger: 'bg-[var(--color-danger)] text-white hover:opacity-90 focus:ring-red-500',
      ghost: 'text-[var(--color-text)] hover:bg-[var(--color-surface-overlay)] focus:ring-[var(--color-border-focus)]',
    };
    return [base, sizes[this.size], variants[this.variant]].join(' ');
  }
}
