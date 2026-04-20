import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TableColumn<T = unknown> {
  key: string;
  label: string;
  render?: (row: T) => string;
}

@Component({
  selector: 'app-table',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
      <table class="w-full text-sm">
        <thead class="bg-[var(--color-surface-raised)] border-b border-[var(--color-border)]">
          <tr>
            @for (col of columns; track col.key) {
              <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">{{ col.label }}</th>
            }
            @if (hasActions) {
              <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Actions</th>
            }
          </tr>
        </thead>
        <tbody class="divide-y divide-[var(--color-border)]">
          @for (row of rows; track trackBy(row)) {
            <tr class="hover:bg-[var(--color-surface-raised)]">
              @for (col of columns; track col.key) {
                <td class="px-4 py-3 text-[var(--color-text)]">
                  {{ col.render ? col.render(row) : getCell(row, col.key) }}
                </td>
              }
              @if (hasActions) {
                <td class="px-4 py-3 text-right">
                  <ng-container *ngTemplateOutlet="actionTpl; context: { $implicit: row }" />
                </td>
              }
            </tr>
          }
          @if (rows.length === 0 && !loading) {
            <tr>
              <td [attr.colspan]="columns.length + (hasActions ? 1 : 0)"
                  class="px-4 py-8 text-center text-[var(--color-text-muted)]">
                {{ emptyMessage }}
              </td>
            </tr>
          }
          @if (loading) {
            @for (i of skeletonRows; track i) {
              <tr>
                @for (col of columns; track col.key) {
                  <td class="px-4 py-3">
                    <div class="animate-pulse h-4 rounded bg-[var(--color-surface-overlay)]"></div>
                  </td>
                }
              </tr>
            }
          }
        </tbody>
      </table>
    </div>
  `,
})
export class TableComponent<T extends Record<string, unknown>> {
  @Input() columns: TableColumn<T>[] = [];
  @Input() rows: T[] = [];
  @Input() loading = false;
  @Input() emptyMessage = 'No records found.';
  @Input() hasActions = false;
  @Input() actionTpl: unknown = null;
  @Input() idKey = 'id';

  skeletonRows = [1, 2, 3, 4, 5];

  trackBy(row: T): unknown {
    return row[this.idKey] ?? Math.random();
  }

  getCell(row: T, key: string): string {
    const val = row[key];
    return val !== null && val !== undefined ? String(val) : '';
  }
}
