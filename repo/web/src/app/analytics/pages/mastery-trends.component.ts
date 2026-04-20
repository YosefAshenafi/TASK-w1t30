import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface MasteryTrendPoint { at: string; masteryPct: number; attempts: number; }
interface MasteryTrendSeries { scope: string; points: MasteryTrendPoint[]; }

@Component({
  selector: 'app-mastery-trends',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Mastery Trends</h1>

      <form [formGroup]="filters" class="grid grid-cols-1 md:grid-cols-3 gap-3 mb-6">
        <input formControlName="learnerId" type="text" placeholder="Learner ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="courseId" type="text" placeholder="Course ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="courseVersion" type="text" placeholder="Course version (e.g. 2024.1)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="locationId" type="text" placeholder="Location ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="instructorId" type="text" placeholder="Instructor ID (optional)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="cohortId" type="text" placeholder="Cohort ID (optional)"
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
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Mastery</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Attempts</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Bar</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (row of data; track row.at) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3">{{ formatDate(row.at) }}</td>
                  <td class="px-4 py-3 font-medium">{{ row.masteryPct.toFixed(0) }}%</td>
                  <td class="px-4 py-3">{{ row.attempts }}</td>
                  <td class="px-4 py-3 w-48">
                    <div class="h-2 rounded-full bg-[var(--color-surface-overlay)]">
                      <div class="h-2 rounded-full bg-[var(--color-brand-500)]" [style.width]="row.masteryPct + '%'"></div>
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
  data: MasteryTrendPoint[] = [];
  loading = false;

  constructor(private fb: FormBuilder, private http: HttpClient, private authStore: AuthStore) {
    this.filters = this.fb.group({
      learnerId: [''],
      courseId: [''],
      courseVersion: [''],
      locationId: [''],
      instructorId: [''],
      cohortId: [''],
      from: [''],
      to: [''],
    });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const { learnerId, courseId, courseVersion, locationId, instructorId, cohortId, from, to } =
      this.filters.value;
    const role = this.authStore.userRole();

    const parts: string[] = [];
    const resolvedLearner = role === 'STUDENT' ? this.authStore.userId() : learnerId;
    if (resolvedLearner) parts.push(`learnerId=${encodeURIComponent(resolvedLearner)}`);
    if (courseId) parts.push(`courseId=${encodeURIComponent(courseId)}`);
    if (courseVersion) parts.push(`courseVersion=${encodeURIComponent(courseVersion)}`);
    if (locationId) parts.push(`locationId=${encodeURIComponent(locationId)}`);
    if (instructorId) parts.push(`instructorId=${encodeURIComponent(instructorId)}`);
    if (cohortId) parts.push(`cohortId=${encodeURIComponent(cohortId)}`);
    if (from) parts.push(`from=${from}T00:00:00Z`);
    if (to) parts.push(`to=${to}T23:59:59Z`);
    const qs = parts.join('&');

    this.http.get<MasteryTrendSeries>(`/api/v1/analytics/mastery-trends?${qs}`).pipe(
      catchError(() => of({ scope: '', points: [] } as MasteryTrendSeries))
    ).subscribe(r => { this.data = r.points; this.loading = false; });
  }

  formatDate(d: string): string {
    return new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }
}
