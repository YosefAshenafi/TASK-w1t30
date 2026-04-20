import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface MasteryPoint { date: string; knowledgePointId: string; score: number; }

@Component({
  selector: 'app-mastery-trends',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Mastery Trends</h1>

      <form [formGroup]="filters" class="flex flex-wrap gap-3 mb-6">
        <input formControlName="learnerId" type="text" placeholder="Learner ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="from" type="date"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="to" type="date"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <button type="button" (click)="load()"
          class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm min-h-[48px] hover:bg-[var(--color-brand-700)]">
          Apply
        </button>
      </form>

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3,4]; track i) { <app-skeleton height="48px" /> }
        </div>
      } @else if (data.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm">No mastery data found for the selected filters.</p>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
          <table class="w-full text-sm">
            <thead class="bg-[var(--color-surface-raised)]">
              <tr>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Date</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Knowledge Point</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Score</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Bar</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (row of data; track row.date + row.knowledgePointId) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3">{{ formatDate(row.date) }}</td>
                  <td class="px-4 py-3 font-mono text-xs">{{ row.knowledgePointId }}</td>
                  <td class="px-4 py-3 font-medium">{{ (row.score * 100).toFixed(0) }}%</td>
                  <td class="px-4 py-3 w-48">
                    <div class="h-2 rounded-full bg-[var(--color-surface-overlay)]">
                      <div class="h-2 rounded-full bg-[var(--color-brand-500)]" [style.width]="(row.score * 100) + '%'"></div>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class MasteryTrendsComponent implements OnInit {
  filters: FormGroup;
  data: MasteryPoint[] = [];
  loading = false;

  constructor(private fb: FormBuilder, private http: HttpClient, private authStore: AuthStore) {
    this.filters = this.fb.group({ learnerId: [''], from: [''], to: [''] });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const { learnerId, from, to } = this.filters.value;
    const role = this.authStore.userRole();

    let params = '';
    if (role === 'STUDENT') {
      params += `learnerId=${this.authStore.userId()}`;
    } else if (learnerId) {
      params += `learnerId=${learnerId}`;
    }
    if (from) params += `&from=${from}`;
    if (to) params += `&to=${to}`;

    this.http.get<MasteryPoint[]>(`/api/v1/analytics/mastery-trends?${params}`).pipe(
      catchError(() => of([]))
    ).subscribe(d => { this.data = d; this.loading = false; });
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }
}
