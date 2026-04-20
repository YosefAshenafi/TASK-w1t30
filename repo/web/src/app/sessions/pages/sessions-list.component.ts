import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { NetworkStatusService } from '../../core/stores/network-status.service';
import { SessionStore } from '../session.store';
import { BannerComponent } from '../../shared/ui/banner.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { SessionRecord } from '../../core/db/dexie';
import { catchError, of } from 'rxjs';

interface SessionRow {
  id: string;
  courseId: string;
  startedAt: string;
  status: string;
  clientUpdatedAt: string;
}

@Component({
  selector: 'app-sessions-list',
  standalone: true,
  imports: [CommonModule, RouterLink, BannerComponent, ButtonComponent, SkeletonComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-xl font-bold">Training Sessions</h1>
        <a routerLink="/sessions/new">
          <app-button variant="primary">+ New Session</app-button>
        </a>
      </div>

      @if (!(network.online$ | async)) {
        <app-banner message="Offline — showing cached sessions. Changes will sync when back online." severity="warn" />
      }

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3]; track i) {
            <app-skeleton height="64px" extraClass="rounded-xl" />
          }
        </div>
      } @else {
        <div class="flex flex-col gap-3">
          @for (session of sessions; track session.id) {
            <a
              [routerLink]="['/sessions', session.id, 'run']"
              class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4 hover:border-[var(--color-brand-500)] transition-colors min-h-0">
              <div>
                <p class="font-medium text-sm">Session {{ session.id.slice(-8) }}</p>
                <p class="text-xs text-[var(--color-text-muted)] mt-0.5">
                  Started {{ formatDate(session.startedAt) }}
                </p>
              </div>
              <span [class]="statusClass(session.status)" class="text-xs font-medium px-2 py-1 rounded-full">
                {{ session.status }}
              </span>
            </a>
          }
          @if (sessions.length === 0) {
            <div class="text-center py-12 text-[var(--color-text-muted)]">
              <p class="text-base mb-2">No sessions yet.</p>
              <a routerLink="/sessions/new" class="text-[var(--color-brand-600)] hover:underline text-sm">Start your first session →</a>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class SessionsListComponent implements OnInit {
  sessions: SessionRow[] = [];
  loading = true;

  constructor(
    private http: HttpClient,
    private authStore: AuthStore,
    private sessionStore: SessionStore,
    readonly network: NetworkStatusService,
  ) {}

  ngOnInit(): void {
    const userId = this.authStore.userId()!;

    if (!this.network.isOnline()) {
      this.loadFromCache(userId);
      return;
    }

    this.http.get<{ content: SessionRow[] }>(`/api/v1/sessions?learnerId=${userId}&size=50`).pipe(
      catchError(() => of(null))
    ).subscribe(res => {
      if (res) {
        this.sessions = res.content;
      } else {
        this.loadFromCache(userId);
      }
      this.loading = false;
    });
  }

  private loadFromCache(userId: string): void {
    this.sessionStore.loadActiveSessions(userId).then(() => {
      this.sessionStore.activeSessions$.subscribe(records => {
        this.sessions = records as SessionRow[];
        this.loading = false;
      });
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      IN_PROGRESS: 'bg-green-100 text-green-800',
      PAUSED: 'bg-amber-100 text-amber-800',
      COMPLETED: 'bg-sky-100 text-sky-800',
      ABANDONED: 'bg-gray-100 text-gray-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }
}
