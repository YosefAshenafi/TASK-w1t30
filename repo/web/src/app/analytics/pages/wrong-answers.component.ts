import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface WrongAnswerItem {
  itemId: string;
  stemPreview: string;
  wrongChoiceId: string;
  count: number;
  pct: number;
}

interface WrongAnswerResponse {
  items: WrongAnswerItem[];
}

@Component({
  selector: 'app-wrong-answers',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Wrong Answers</h1>

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
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3,4]; track i) { <app-skeleton height="80px" /> }
        </div>
      } @else if (data.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm">No wrong answers found.</p>
      } @else {
        <div class="flex flex-col gap-3">
          @for (row of data; track row.itemId + '_' + row.wrongChoiceId) {
            <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <p class="text-sm font-medium mb-2">{{ row.stemPreview }}</p>
                  <div class="flex gap-6 text-xs">
                    <span class="text-red-700"><span class="font-medium">Wrong choice:</span> {{ row.wrongChoiceId }}</span>
                    <span class="text-[var(--color-text-muted)]"><span class="font-medium">Share:</span> {{ (row.pct * 100).toFixed(1) }}%</span>
                  </div>
                </div>
                <span class="text-2xl font-bold text-[var(--color-text-muted)] flex-shrink-0">{{ row.count }}×</span>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class WrongAnswersComponent implements OnInit {
  filters: FormGroup;
  data: WrongAnswerItem[] = [];
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

    this.http.get<WrongAnswerResponse>(`/api/v1/analytics/wrong-answers?${parts.join('&')}`).pipe(
      catchError(() => of({ items: [] as WrongAnswerItem[] }))
    ).subscribe(d => { this.data = d.items ?? []; this.loading = false; });
  }
}
