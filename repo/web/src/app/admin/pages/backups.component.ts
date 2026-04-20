import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { TabsComponent, Tab } from '../../shared/ui/tabs.component';
import { catchError, of } from 'rxjs';

interface BackupRun {
  id: string;
  type: string;
  status: string;
  sizeBytes: number | null;
  startedAt: string;
  completedAt: string | null;
  errorMessage: string | null;
}

interface RecoveryDrill {
  id: string;
  backupRunId: string;
  status: string;
  notes: string | null;
  scheduledAt: string;
  completedAt: string | null;
}

@Component({
  selector: 'app-admin-backups',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent, BannerComponent, TabsComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <div class="flex items-center justify-between mb-4">
        <h1 class="text-xl font-bold">Backups & Recovery</h1>
        <div class="flex gap-2">
          <app-button variant="primary" [loading]="triggeringFull" (click)="triggerBackup('FULL')">Run Full Backup</app-button>
          <app-button variant="secondary" [loading]="triggeringIncr" (click)="triggerBackup('INCREMENTAL')">Incremental</app-button>
          <app-button variant="secondary" [loading]="triggeringDrill" (click)="triggerDrill()">Run Drill</app-button>
        </div>
      </div>

      @if (message) {
        <app-banner [message]="message" [severity]="messageType" [dismissible]="true" (dismissed)="message=''" />
      }

      <app-tabs [tabs]="tabs" [active]="activeTab" (activeChange)="setTab($event)" />

      <div class="mt-4">
        @if (loading) {
          <div class="flex flex-col gap-3">
            @for (i of [1,2,3]; track i) { <app-skeleton height="56px" /> }
          </div>
        } @else if (activeTab === 'backups') {
          <div class="flex flex-col gap-2">
            @for (run of backupRuns; track run.id) {
              <div class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-sm">{{ run.type }}</span>
                    <span [class]="statusClass(run.status)" class="text-xs px-2 py-1 rounded-full font-medium">
                      {{ run.status }}
                    </span>
                  </div>
                  <p class="text-xs text-[var(--color-text-muted)] mt-0.5">
                    Started {{ formatDate(run.startedAt) }}
                    @if (run.sizeBytes) { · {{ formatSize(run.sizeBytes) }} }
                    @if (run.errorMessage) { · ⚠ {{ run.errorMessage }} }
                  </p>
                </div>
              </div>
            }
            @if (backupRuns.length === 0) {
              <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">No backup runs yet.</p>
            }
          </div>
        } @else {
          <div class="flex flex-col gap-2">
            @for (drill of drills; track drill.id) {
              <div class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-medium text-sm">Recovery Drill</span>
                    <span [class]="statusClass(drill.status)" class="text-xs px-2 py-1 rounded-full font-medium">
                      {{ drill.status }}
                    </span>
                  </div>
                  <p class="text-xs text-[var(--color-text-muted)] mt-0.5">
                    Scheduled {{ formatDate(drill.scheduledAt) }}
                    @if (drill.notes) { · {{ drill.notes }} }
                  </p>
                </div>
              </div>
            }
            @if (drills.length === 0) {
              <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">No recovery drills yet.</p>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class AdminBackupsComponent implements OnInit {
  backupRuns: BackupRun[] = [];
  drills: RecoveryDrill[] = [];
  loading = false;
  triggeringFull = false;
  triggeringIncr = false;
  triggeringDrill = false;
  message = '';
  messageType: 'success' | 'info' | 'error' = 'success';
  activeTab = 'backups';
  tabs: Tab[] = [
    { id: 'backups', label: 'Backup Runs' },
    { id: 'drills', label: 'Recovery Drills' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.loadBackups(); this.loadDrills(); }

  setTab(id: string): void {
    this.activeTab = id;
    if (id === 'backups') this.loadBackups();
    else this.loadDrills();
  }

  loadBackups(): void {
    this.loading = true;
    this.http.get<{ content: BackupRun[] }>('/api/v1/admin/backups').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.backupRuns = r.content; this.loading = false; });
  }

  loadDrills(): void {
    this.loading = true;
    this.http.get<{ content: RecoveryDrill[] }>('/api/v1/admin/backups/recovery-drills').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.drills = r.content; this.loading = false; });
  }

  triggerBackup(mode: string): void {
    if (mode === 'FULL') this.triggeringFull = true;
    else this.triggeringIncr = true;

    this.http.post<BackupRun>(`/api/v1/admin/backups/run?mode=${mode}`, {}).pipe(
      catchError(() => of(null))
    ).subscribe(run => {
      if (mode === 'FULL') this.triggeringFull = false;
      else this.triggeringIncr = false;
      if (run) { this.message = `${mode} backup started.`; this.messageType = 'info'; this.loadBackups(); }
    });
  }

  triggerDrill(): void {
    this.triggeringDrill = true;
    this.http.post<RecoveryDrill>('/api/v1/admin/backups/recovery-drill', {}).pipe(
      catchError(() => of(null))
    ).subscribe(drill => {
      this.triggeringDrill = false;
      if (drill) { this.message = 'Recovery drill started.'; this.messageType = 'info'; this.loadDrills(); }
      else { this.message = 'No completed backup available for drill.'; this.messageType = 'error'; }
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      RUNNING: 'bg-blue-100 text-blue-800',
      COMPLETED: 'bg-green-100 text-green-800',
      PASSED: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      PENDING: 'bg-amber-100 text-amber-800',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}
