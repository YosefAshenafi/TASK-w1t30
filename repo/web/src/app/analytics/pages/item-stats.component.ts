import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface ItemStatItem {
  itemId: string;
  difficulty: number;
  discrimination: number;
  attempts: number;
}

interface ItemStatResponse {
  items: ItemStatItem[];
}

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
        <input formControlName="courseVersion" type="text" placeholder="Course version"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="locationId" type="text" placeholder="Location ID"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="instructorId" type="text" placeholder="Instructor ID"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="from" type="date"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="to" type="date"
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
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Difficulty</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Discrimination</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (row of data; track row.itemId) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3 font-mono text-xs">{{ row.itemId }}</td>
                  <td class="px-4 py-3 text-right">{{ row.attempts }}</td>
                  <td class="px-4 py-3 text-right font-medium"
                    [class.text-red-700]="row.difficulty >= 0.7"
                    [class.text-green-700]="row.difficulty < 0.4">
                    {{ row.difficulty.toFixed(2) }}
                  </td>
                  <td class="px-4 py-3 text-right">{{ row.discrimination.toFixed(2) }}</td>
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
  data: ItemStatItem[] = [];
  loading = false;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.filters = this.fb.group({
      courseId: [''], courseVersion: [''], locationId: [''],
      instructorId: [''], from: [''], to: [''],
    });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const v = this.filters.value;
    const parts: string[] = [];
    if (v.courseId) parts.push(`courseId=${v.courseId}`);
    if (v.courseVersion) parts.push(`courseVersion=${encodeURIComponent(v.courseVersion)}`);
    if (v.locationId) parts.push(`locationId=${v.locationId}`);
    if (v.instructorId) parts.push(`instructorId=${v.instructorId}`);
    if (v.from) parts.push(`from=${new Date(v.from).toISOString()}`);
    if (v.to) parts.push(`to=${new Date(v.to).toISOString()}`);
    const qs = parts.length ? `?${parts.join('&')}` : '';
    this.http.get<ItemStatResponse>(`/api/v1/analytics/item-stats${qs}`).pipe(
      catchError(() => of({ items: [] as ItemStatItem[] }))
    ).subscribe(d => { this.data = d.items ?? []; this.loading = false; });
  }
}
