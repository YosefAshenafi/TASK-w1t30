import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface ItemStat { itemId: string; attempts: number; correctRate: number; avgTimeSeconds: number; }

@Component({
  selector: 'app-item-stats',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Assessment Item Statistics</h1>

      <form [formGroup]="filters" class="flex flex-wrap gap-3 mb-6">
        <input formControlName="courseId" type="text" placeholder="Course ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <button type="button" (click)="load()"
          class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm min-h-[48px]">
          Apply
        </button>
      </form>

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3,4,5]; track i) { <app-skeleton height="48px" /> }
        </div>
      } @else if (data.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm">No item statistics available.</p>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
          <table class="w-full text-sm">
            <thead class="bg-[var(--color-surface-raised)]">
              <tr>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Item ID</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Attempts</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Correct Rate</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Avg Time (s)</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (row of data; track row.itemId) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3 font-mono text-xs">{{ row.itemId }}</td>
                  <td class="px-4 py-3 text-right">{{ row.attempts }}</td>
                  <td class="px-4 py-3 text-right font-medium"
                    [class.text-green-700]="row.correctRate >= 0.7"
                    [class.text-red-700]="row.correctRate < 0.4">
                    {{ (row.correctRate * 100).toFixed(1) }}%
                  </td>
                  <td class="px-4 py-3 text-right">{{ row.avgTimeSeconds.toFixed(0) }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class ItemStatsComponent implements OnInit {
  filters: FormGroup;
  data: ItemStat[] = [];
  loading = false;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.filters = this.fb.group({ courseId: [''] });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const { courseId } = this.filters.value;
    const params = courseId ? `?courseId=${courseId}` : '';
    this.http.get<ItemStat[]>(`/api/v1/analytics/item-stats${params}`).pipe(
      catchError(() => of([]))
    ).subscribe(d => { this.data = d; this.loading = false; });
  }
}
