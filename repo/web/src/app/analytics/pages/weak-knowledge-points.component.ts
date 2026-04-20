import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface WeakKnowledgePointItem {
  knowledgePointId: string;
  name: string;
  masteryPct: number;
  attemptVolume: number;
}

interface WeakKnowledgePointResponse {
  items: WeakKnowledgePointItem[];
}

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
        <input formControlName="courseId" type="text" placeholder="Course ID"
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
                <span class="text-red-700 font-semibold text-sm">{{ row.masteryPct.toFixed(0) }}%</span>
              </div>
              <div class="h-2 rounded-full bg-[var(--color-surface-overlay)]">
                <div class="h-2 rounded-full bg-red-400" [style.width]="row.masteryPct + '%'"></div>
              </div>
              <p class="text-xs text-[var(--color-text-muted)] mt-1">{{ row.attemptVolume }} attempts</p>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class WeakKnowledgePointsComponent implements OnInit {
  filters: FormGroup;
  data: WeakKnowledgePointItem[] = [];
  loading = false;
  canFilter = false;

  constructor(private fb: FormBuilder, private http: HttpClient, private authStore: AuthStore) {
    this.filters = this.fb.group({
      learnerId: [''], courseId: [''], courseVersion: [''],
      locationId: [''], instructorId: [''], from: [''], to: [''],
    });
  }

  ngOnInit(): void {
    this.canFilter = this.authStore.userRole() !== 'STUDENT';
    this.load();
  }

  load(): void {
    this.loading = true;
    const v = this.filters.value;
    const role = this.authStore.userRole();
    const parts: string[] = [];
    if (role === 'STUDENT') {
      parts.push(`learnerId=${this.authStore.userId()}`);
    } else if (v.learnerId) {
      parts.push(`learnerId=${v.learnerId}`);
    }
    if (v.courseId) parts.push(`courseId=${v.courseId}`);
    if (v.courseVersion) parts.push(`courseVersion=${encodeURIComponent(v.courseVersion)}`);
    if (v.locationId) parts.push(`locationId=${v.locationId}`);
    if (v.instructorId) parts.push(`instructorId=${v.instructorId}`);
    if (v.from) parts.push(`from=${new Date(v.from).toISOString()}`);
    if (v.to) parts.push(`to=${new Date(v.to).toISOString()}`);

    this.http.get<WeakKnowledgePointResponse>(`/api/v1/analytics/weak-knowledge-points?${parts.join('&')}`).pipe(
      catchError(() => of({ items: [] as WeakKnowledgePointItem[] }))
    ).subscribe(d => { this.data = d.items ?? []; this.loading = false; });
  }
}
