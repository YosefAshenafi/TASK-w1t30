import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface WrongAnswer { itemId: string; question: string; givenAnswer: string; correctAnswer: string; count: number; }

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
          @for (row of data; track row.itemId) {
            <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div class="flex items-start justify-between gap-4">
                <div class="flex-1">
                  <p class="text-sm font-medium mb-2">{{ row.question }}</p>
                  <div class="flex gap-6 text-xs">
                    <span class="text-red-700"><span class="font-medium">Given:</span> {{ row.givenAnswer }}</span>
                    <span class="text-green-700"><span class="font-medium">Correct:</span> {{ row.correctAnswer }}</span>
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
  data: WrongAnswer[] = [];
  loading = false;
  canFilter = false;

  constructor(private fb: FormBuilder, private http: HttpClient, private authStore: AuthStore) {
    this.filters = this.fb.group({ learnerId: [''], courseId: [''] });
  }

  ngOnInit(): void {
    this.canFilter = this.authStore.userRole() !== 'STUDENT';
    this.load();
  }

  load(): void {
    this.loading = true;
    const { learnerId, courseId } = this.filters.value;
    const role = this.authStore.userRole();
    let params = '';
    if (role === 'STUDENT') {
      params += `learnerId=${this.authStore.userId()}`;
    } else if (learnerId) {
      params += `learnerId=${learnerId}`;
    }
    if (courseId) params += `${params ? '&' : ''}courseId=${courseId}`;

    this.http.get<WrongAnswer[]>(`/api/v1/analytics/wrong-answers?${params}`).pipe(
      catchError(() => of([]))
    ).subscribe(d => { this.data = d; this.loading = false; });
  }
}
