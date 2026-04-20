import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface WeakPoint { knowledgePointId: string; name: string; avgScore: number; attemptCount: number; }

@Component({
  selector: 'app-weak-knowledge-points',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Weak Knowledge Points</h1>

      <form [formGroup]="filters" class="flex flex-wrap gap-3 mb-6">
        @if (canFilter) {
          <input formControlName="learnerId" type="text" placeholder="Learner ID"
            class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        }
        <input formControlName="threshold" type="number" min="0" max="1" step="0.05" placeholder="Threshold (0-1)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px] w-44" />
        <button type="button" (click)="load()"
          class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm min-h-[48px]">
          Apply
        </button>
      </form>

      @if (loading) {
        <div class="flex flex-col gap-2">
          @for (i of [1,2,3]; track i) { <app-skeleton height="56px" /> }
        </div>
      } @else if (data.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm">No weak knowledge points found.</p>
      } @else {
        <div class="flex flex-col gap-2">
          @for (row of data; track row.knowledgePointId) {
            <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div class="flex items-center justify-between mb-2">
                <p class="font-medium text-sm">{{ row.name || row.knowledgePointId }}</p>
                <span class="text-red-700 font-semibold text-sm">{{ (row.avgScore * 100).toFixed(0) }}%</span>
              </div>
              <div class="h-2 rounded-full bg-[var(--color-surface-overlay)]">
                <div class="h-2 rounded-full bg-red-400" [style.width]="(row.avgScore * 100) + '%'"></div>
              </div>
              <p class="text-xs text-[var(--color-text-muted)] mt-1">{{ row.attemptCount }} attempts</p>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class WeakKnowledgePointsComponent implements OnInit {
  filters: FormGroup;
  data: WeakPoint[] = [];
  loading = false;
  canFilter = false;

  constructor(private fb: FormBuilder, private http: HttpClient, private authStore: AuthStore) {
    this.filters = this.fb.group({ learnerId: [''], threshold: [0.6] });
  }

  ngOnInit(): void {
    this.canFilter = this.authStore.userRole() !== 'STUDENT';
    this.load();
  }

  load(): void {
    this.loading = true;
    const { learnerId, threshold } = this.filters.value;
    const role = this.authStore.userRole();
    let params = `threshold=${threshold ?? 0.6}`;
    if (role === 'STUDENT') {
      params += `&learnerId=${this.authStore.userId()}`;
    } else if (learnerId) {
      params += `&learnerId=${learnerId}`;
    }

    this.http.get<WeakPoint[]>(`/api/v1/analytics/weak-knowledge-points?${params}`).pipe(
      catchError(() => of([]))
    ).subscribe(d => { this.data = d; this.loading = false; });
  }
}
