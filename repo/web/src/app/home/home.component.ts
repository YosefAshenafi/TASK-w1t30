import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../core/stores/auth.store';
import { SkeletonComponent } from '../shared/ui/skeleton.component';
import { BannerComponent } from '../shared/ui/banner.component';
import { NetworkStatusService } from '../core/stores/network-status.service';
import { catchError, of } from 'rxjs';

interface DashboardStats {
  activeSessions?: number;
  completedSessions?: number;
  pendingApprovals?: number;
  unresolvedAnomalies?: number;
  unreadNotifications?: number;
  certExpiringSoon?: number;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <div class="mb-6">
        <h1 class="text-2xl font-bold text-[var(--color-text)]">
          Welcome, {{ (authStore.profile$ | async)?.displayName ?? 'User' }}
        </h1>
        <p class="text-sm text-[var(--color-text-muted)] mt-1">{{ roleLabel() }}</p>
      </div>

      @if (!(networkStatus.online$ | async)) {
        <app-banner message="You are offline. Data shown may be from cache." severity="warn" class="block mb-4" />
      }

      @if (loading) {
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (i of [1,2,3]; track i) {
            <div class="bg-[var(--color-surface-raised)] rounded-xl p-5 border border-[var(--color-border)]">
              <app-skeleton height="1rem" extraClass="mb-2 w-1/2" />
              <app-skeleton height="2.5rem" extraClass="w-1/3" />
            </div>
          }
        </div>
      } @else {
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">

          <!-- Student widgets -->
          @if (role === 'STUDENT') {
            <a routerLink="/sessions"
              class="bg-[var(--color-brand-50)] rounded-xl p-5 border border-[var(--color-brand-100)] hover:border-[var(--color-brand-500)] transition-colors block min-h-0">
              <p class="text-xs font-medium text-[var(--color-text-muted)] mb-1">Active Sessions</p>
              <p class="text-3xl font-bold text-[var(--color-brand-600)]">{{ stats.activeSessions ?? 0 }}</p>
            </a>
            <a routerLink="/sessions"
              class="bg-[var(--color-surface-raised)] rounded-xl p-5 border border-[var(--color-border)] hover:border-[var(--color-brand-500)] transition-colors block min-h-0">
              <p class="text-xs font-medium text-[var(--color-text-muted)] mb-1">Completed Sessions</p>
              <p class="text-3xl font-bold text-[var(--color-text)]">{{ stats.completedSessions ?? 0 }}</p>
            </a>
          }

          <!-- Mentor / Admin analytics widget -->
          @if (role !== 'STUDENT') {
            <a routerLink="/analytics/mastery"
              class="bg-[var(--color-brand-50)] rounded-xl p-5 border border-[var(--color-brand-100)] hover:border-[var(--color-brand-500)] transition-colors block min-h-0">
              <p class="text-xs font-medium text-[var(--color-text-muted)] mb-1">Analytics</p>
              <p class="text-base font-semibold text-[var(--color-brand-600)]">View mastery trends →</p>
            </a>
            <a routerLink="/reports"
              class="bg-[var(--color-surface-raised)] rounded-xl p-5 border border-[var(--color-border)] hover:border-[var(--color-brand-500)] transition-colors block min-h-0">
              <p class="text-xs font-medium text-[var(--color-text-muted)] mb-1">Reports</p>
              <p class="text-base font-semibold text-[var(--color-text)]">Generate reports →</p>
            </a>
          }

          <!-- Admin-only widgets -->
          @if (role === 'ADMIN') {
            <a routerLink="/admin/approvals"
              class="bg-amber-50 rounded-xl p-5 border border-amber-200 hover:border-amber-400 transition-colors block min-h-0">
              <p class="text-xs font-medium text-amber-700 mb-1">Pending Approvals</p>
              <p class="text-3xl font-bold text-amber-700">{{ stats.pendingApprovals ?? 0 }}</p>
            </a>
            <a routerLink="/admin/anomalies"
              class="bg-red-50 rounded-xl p-5 border border-red-200 hover:border-red-400 transition-colors block min-h-0">
              <p class="text-xs font-medium text-red-700 mb-1">Unresolved Anomalies</p>
              <p class="text-3xl font-bold text-red-700">{{ stats.unresolvedAnomalies ?? 0 }}</p>
            </a>
          }

          <!-- Notifications widget — all roles -->
          <a routerLink="/notifications"
            class="bg-[var(--color-surface-raised)] rounded-xl p-5 border border-[var(--color-border)] hover:border-[var(--color-brand-500)] transition-colors block min-h-0">
            <p class="text-xs font-medium text-[var(--color-text-muted)] mb-1">Unread Notifications</p>
            <p class="text-3xl font-bold text-[var(--color-text)]">{{ stats.unreadNotifications ?? 0 }}</p>
          </a>

        </div>
      }
    </div>
  `,
})
export class HomeComponent implements OnInit {
  loading = true;
  stats: DashboardStats = {};
  role = '';

  constructor(
    readonly authStore: AuthStore,
    readonly networkStatus: NetworkStatusService,
    private http: HttpClient,
  ) {}

  ngOnInit(): void {
    this.role = this.authStore.userRole() ?? '';
    this.loadStats();
  }

  roleLabel(): string {
    const map: Record<string, string> = {
      STUDENT: 'Learner Dashboard',
      CORPORATE_MENTOR: 'Corporate Mentor Dashboard',
      FACULTY_MENTOR: 'Faculty Mentor Dashboard',
      ADMIN: 'Administrator Dashboard',
    };
    return map[this.role] ?? 'Dashboard';
  }

  private loadStats(): void {
    const userId = this.authStore.userId();

    this.http.get<{ unreadCount: number }>('/api/v1/notifications/unread-count').pipe(
      catchError(() => of({ unreadCount: 0 }))
    ).subscribe(r => {
      this.stats.unreadNotifications = r.unreadCount;
    });

    if (this.role === 'STUDENT' && userId) {
      this.http.get<{ content: unknown[] }>(`/api/v1/sessions?learnerId=${userId}&status=IN_PROGRESS&size=1`).pipe(
        catchError(() => of({ content: [] }))
      ).subscribe(r => { this.stats.activeSessions = r.content.length; });

      this.http.get<{ totalElements: number }>(`/api/v1/sessions?learnerId=${userId}&status=COMPLETED&size=1`).pipe(
        catchError(() => of({ totalElements: 0 }))
      ).subscribe(r => { this.stats.completedSessions = r.totalElements; });
    }

    if (this.role === 'ADMIN') {
      this.http.get<{ totalElements: number }>('/api/v1/admin/approvals?status=PENDING&size=1').pipe(
        catchError(() => of({ totalElements: 0 }))
      ).subscribe(r => { this.stats.pendingApprovals = r.totalElements; });

      this.http.get<{ totalElements: number }>('/api/v1/admin/anomalies?resolved=false&size=1').pipe(
        catchError(() => of({ totalElements: 0 }))
      ).subscribe(r => { this.stats.unresolvedAnomalies = r.totalElements; });
    }

    this.loading = false;
  }
}
