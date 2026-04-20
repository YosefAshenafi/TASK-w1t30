import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SelectComponent, SelectOption } from '../../shared/ui/select.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { catchError, of } from 'rxjs';

interface ReportSchedule {
  id: string;
  type: string;
  cronExpr: string;
  ownerId: string;
  enabled: boolean;
  nextRunAt: string | null;
  createdAt: string | null;
}

@Component({
  selector: 'app-schedules',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SelectComponent, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <h1 class="text-xl font-bold mb-6">Report Schedules</h1>

      <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl p-5 mb-6">
        <h2 class="font-semibold mb-4 text-sm">Add Schedule</h2>

        @if (errorMessage) {
          <app-banner [message]="errorMessage" severity="error" [dismissible]="true" (dismissed)="errorMessage=''" />
        }

        <form [formGroup]="form" (ngSubmit)="add()" class="flex flex-wrap gap-3 items-end">
          <div class="flex-1 min-w-36">
            <app-select label="Report type" [options]="typeOptions" formControlName="kind" placeholder="Select…" />
          </div>
          <div class="flex flex-col gap-1 min-w-48">
            <label class="text-sm font-medium">Cron expression</label>
            <input formControlName="cronExpr" type="text" placeholder="0 0 1 * * *"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
          </div>
          <div class="flex-1 min-w-36">
            <app-select label="Format" [options]="formatOptions" formControlName="format" placeholder="Select…" />
          </div>
          <app-button type="submit" variant="primary" [loading]="adding" [disabled]="form.invalid">
            Add
          </app-button>
        </form>
      </div>

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2]; track i) { <app-skeleton height="64px" /> }
        </div>
      } @else if (schedules.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm">No schedules configured.</p>
      } @else {
        <div class="flex flex-col gap-3">
          @for (sched of schedules; track sched.id) {
            <div class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div>
                <p class="font-medium text-sm">{{ sched.type }}</p>
                <p class="text-xs text-[var(--color-text-muted)] font-mono mt-0.5">{{ sched.cronExpr }}</p>
                @if (sched.nextRunAt) {
                  <p class="text-xs text-[var(--color-text-muted)] mt-0.5">Next: {{ formatDate(sched.nextRunAt) }}</p>
                }
              </div>
              <div class="flex items-center gap-3">
                <span [class]="sched.enabled ? 'text-green-700 bg-green-100' : 'text-gray-600 bg-gray-100'"
                  class="text-xs px-2 py-1 rounded-full">
                  {{ sched.enabled ? 'Enabled' : 'Disabled' }}
                </span>
                <button (click)="toggleEnabled(sched)"
                  class="text-xs text-[var(--color-brand-600)] hover:underline min-h-0">
                  {{ sched.enabled ? 'Disable' : 'Enable' }}
                </button>
                <button (click)="remove(sched.id)"
                  class="text-xs text-[var(--color-danger)] hover:underline min-h-0">
                  Delete
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class SchedulesComponent implements OnInit {
  form: FormGroup;
  schedules: ReportSchedule[] = [];
  loading = false;
  adding = false;
  errorMessage = '';

  typeOptions: SelectOption[] = [
    { value: 'ENROLLMENTS', label: 'Enrollments' },
    { value: 'SEAT_UTILIZATION', label: 'Seat Utilisation' },
    { value: 'CERT_EXPIRING', label: 'Expiring Certificates' },
    { value: 'REFUND_RETURN_RATE', label: 'Refund / Return Rate' },
    { value: 'INVENTORY_LEVELS', label: 'Inventory Levels' },
  ];

  formatOptions: SelectOption[] = [
    { value: 'CSV', label: 'CSV' },
    { value: 'PDF', label: 'PDF' },
    { value: 'JSON', label: 'JSON' },
  ];

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.form = this.fb.group({
      kind: ['', Validators.required],
      cronExpr: ['0 0 1 * * *', Validators.required],
      format: ['', Validators.required],
    });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<{ content: ReportSchedule[] }>('/api/v1/reports/schedules').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.schedules = r.content; this.loading = false; });
  }

  add(): void {
    if (this.form.invalid || this.adding) return;
    this.adding = true;
    const { kind, cronExpr, format } = this.form.value;
    this.http.post<ReportSchedule>('/api/v1/reports/schedules', { kind, cronExpr, format }).pipe(
      catchError(err => { this.errorMessage = err.error?.message ?? 'Failed to add schedule.'; return of(null); })
    ).subscribe(s => {
      this.adding = false;
      if (s) { this.schedules = [s, ...this.schedules]; this.form.reset({ cronExpr: '0 0 1 * * *' }); }
    });
  }

  toggleEnabled(sched: ReportSchedule): void {
    this.http.put(`/api/v1/reports/schedules/${sched.id}`, { ...sched, enabled: !sched.enabled }).pipe(
      catchError(() => of(null))
    ).subscribe(() => {
      const idx = this.schedules.findIndex(s => s.id === sched.id);
      if (idx >= 0) this.schedules[idx] = { ...sched, enabled: !sched.enabled };
    });
  }

  remove(id: string): void {
    this.http.delete(`/api/v1/reports/schedules/${id}`).pipe(catchError(() => of(null))).subscribe(() => {
      this.schedules = this.schedules.filter(s => s.id !== id);
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
