import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SelectComponent, SelectOption } from '../../shared/ui/select.component';
import { TabsComponent, Tab } from '../../shared/ui/tabs.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { catchError, of } from 'rxjs';

interface ReportRun {
  id: string;
  type: string;
  status: string;
  filePath: string | null;
  queuedAt: string;
  completedAt: string | null;
  requestedBy: string;
}

const REPORT_TYPES: SelectOption[] = [
  { value: 'ENROLLMENTS', label: 'Enrollments' },
  { value: 'SEAT_UTILIZATION', label: 'Seat Utilisation' },
  { value: 'CERT_EXPIRING', label: 'Expiring Certificates' },
];

@Component({
  selector: 'app-reports-center',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule, ButtonComponent, SelectComponent, TabsComponent, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-6">Reports Center</h1>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <div class="md:col-span-1 bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl p-5">
          <h2 class="font-semibold mb-4 text-sm">Run a Report</h2>

          @if (successMessage) {
            <app-banner [message]="successMessage" severity="success" [dismissible]="true" (dismissed)="successMessage=''" />
          }
          @if (errorMessage) {
            <app-banner [message]="errorMessage" severity="error" [dismissible]="true" (dismissed)="errorMessage=''" />
          }

          <form [formGroup]="form" (ngSubmit)="runReport()" class="flex flex-col gap-4">
            <app-select label="Report type" [options]="reportTypes" placeholder="Select…" formControlName="type" />

            <app-select label="Format" [options]="formatOptions" formControlName="format" />

            <app-button type="submit" variant="primary" [loading]="submitting" [disabled]="form.invalid">
              Generate
            </app-button>
          </form>

          <a routerLink="/reports/schedules" class="block mt-4 text-sm text-[var(--color-brand-600)] hover:underline">
            Manage schedules →
          </a>
        </div>

        <div class="md:col-span-2">
          <app-tabs [tabs]="tabs" [active]="activeTab" (activeChange)="setTab($event)" />

          <div class="mt-4">
            @if (loading) {
              <div class="flex flex-col gap-3">
                @for (i of [1,2,3]; track i) { <app-skeleton height="56px" /> }
              </div>
            } @else if (filteredRuns().length === 0) {
              <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">No reports found.</p>
            } @else {
              <div class="flex flex-col gap-2">
                @for (run of filteredRuns(); track run.id) {
                  <div class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-4 py-3">
                    <div>
                      <p class="text-sm font-medium">{{ run.type }}</p>
                      <p class="text-xs text-[var(--color-text-muted)]">{{ formatDate(run.queuedAt) }}</p>
                    </div>
                    <div class="flex items-center gap-3">
                      <span [class]="statusClass(run.status)" class="text-xs font-medium px-2 py-1 rounded-full">
                        {{ run.status }}
                      </span>
                      @if (run.status === 'COMPLETED' && run.filePath) {
                        <a [href]="downloadUrl(run.id)" target="_blank"
                          class="text-xs text-[var(--color-brand-600)] hover:underline min-h-0">
                          Download
                        </a>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ReportsCenterComponent implements OnInit {
  form: FormGroup;
  runs: ReportRun[] = [];
  loading = false;
  submitting = false;
  successMessage = '';
  errorMessage = '';
  activeTab = 'all';
  reportTypes = REPORT_TYPES;
  formatOptions: SelectOption[] = [
    { value: 'CSV', label: 'CSV' },
    { value: 'PDF', label: 'PDF' },
    { value: 'JSON', label: 'JSON' },
  ];
  tabs: Tab[] = [
    { id: 'all', label: 'All' },
    { id: 'QUEUED', label: 'Queued' },
    { id: 'RUNNING', label: 'Running' },
    { id: 'COMPLETED', label: 'Completed' },
    { id: 'FAILED', label: 'Failed' },
    { id: 'NEEDS_APPROVAL', label: 'Needs Approval' },
  ];

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.form = this.fb.group({
      type: ['', Validators.required],
      format: ['CSV'],
    });
  }

  ngOnInit(): void { this.loadRuns(); }

  loadRuns(): void {
    this.loading = true;
    this.http.get<{ content: ReportRun[] }>('/api/v1/reports?size=50').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.runs = r.content; this.loading = false; });
  }

  runReport(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    const { type, format } = this.form.value;
    this.http.post<ReportRun>('/api/v1/reports', { type, format }).pipe(
      catchError(err => {
        this.errorMessage = err.error?.message ?? 'Report request failed.';
        return of(null);
      })
    ).subscribe(run => {
      this.submitting = false;
      if (run) {
        this.successMessage = run.status === 'NEEDS_APPROVAL'
          ? 'Report queued for approval.'
          : 'Report queued successfully.';
        this.runs = [run, ...this.runs];
      }
    });
  }

  setTab(id: string): void { this.activeTab = id; }

  filteredRuns(): ReportRun[] {
    if (this.activeTab === 'all') return this.runs;
    return this.runs.filter(r => r.status === this.activeTab);
  }

  downloadUrl(id: string): string {
    return `/api/v1/reports/${id}/download`;
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      QUEUED: 'bg-sky-100 text-sky-800',
      RUNNING: 'bg-blue-100 text-blue-800',
      COMPLETED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      NEEDS_APPROVAL: 'bg-amber-100 text-amber-800',
      CANCELLED: 'bg-gray-100 text-gray-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }
}
