import { Component, Input, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

export interface SelectOption {
  value: string | number;
  label: string;
}

@Component({
  selector: 'app-select',
  standalone: true,
  imports: [CommonModule],
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => SelectComponent), multi: true }],
  template: `
    <div class="flex flex-col gap-1">
      @if (label) {
        <label class="text-sm font-medium text-[var(--color-text)]">{{ label }}</label>
      }
      <select
        [disabled]="isDisabled"
        (change)="onChangeEvent($event)"
        (blur)="onTouched()"
        class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px] bg-[var(--color-surface)] disabled:opacity-60">
        @if (placeholder) {
          <option value="">{{ placeholder }}</option>
        }
        @for (opt of options; track opt.value) {
          <option [value]="opt.value" [selected]="opt.value === value">{{ opt.label }}</option>
        }
      </select>
    </div>
  `,
})
export class SelectComponent implements ControlValueAccessor {
  @Input() label = '';
  @Input() placeholder = '';
  @Input() options: SelectOption[] = [];

  value: string | number = '';
  isDisabled = false;
  onChange = (_: string) => {};
  onTouched = () => {};

  writeValue(v: string | number): void { this.value = v; }
  registerOnChange(fn: (_: string) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
  setDisabledState(d: boolean): void { this.isDisabled = d; }

  onChangeEvent(event: Event): void {
    const val = (event.target as HTMLSelectElement).value;
    this.value = val;
    this.onChange(val);
  }
}
