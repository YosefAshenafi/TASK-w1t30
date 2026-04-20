import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => InputComponent), multi: true }],
  template: `
    <div class="flex flex-col gap-1">
      @if (label) {
        <label class="text-sm font-medium text-[var(--color-text)]">{{ label }}</label>
      }
      <input
        [type]="type"
        [placeholder]="placeholder"
        [disabled]="isDisabled"
        [value]="value"
        (input)="onInput($event)"
        (blur)="onTouched()"
        class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px] disabled:bg-[var(--color-surface-raised)] disabled:opacity-60" />
      @if (hint) {
        <p class="text-xs text-[var(--color-text-muted)]">{{ hint }}</p>
      }
      @if (error) {
        <p class="text-xs text-[var(--color-danger)]">{{ error }}</p>
      }
    </div>
  `,
})
export class InputComponent implements ControlValueAccessor {
  @Input() label = '';
  @Input() type = 'text';
  @Input() placeholder = '';
  @Input() hint = '';
  @Input() error = '';

  value = '';
  isDisabled = false;
  onChange = (_: string) => {};
  onTouched = () => {};

  writeValue(v: string): void { this.value = v ?? ''; }
  registerOnChange(fn: (_: string) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.isDisabled = d; }

  onInput(e: Event): void {
    this.value = (e.target as HTMLInputElement).value;
    this.onChange(this.value);
  }
}
