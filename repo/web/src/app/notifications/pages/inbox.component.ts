import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { TabsComponent, Tab } from '../../shared/ui/tabs.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { interval, Subscription, catchError, of, switchMap } from 'rxjs';

interface Notification {
  id: string;
  templateKey: string;
  payload: string;
  severity: string;
  readAt: string | null;
  createdAt: string;
}

@Component({
  selector: 'app-notifications-inbox',
  standalone: true,
  imports: [CommonModule, TabsComponent, SkeletonComponent, ButtonComponent],
  template: `
    <div class="p-6 max-w-3xl mx-auto">
      <div class="flex items-center justify-between mb-4">
        <h1 class="text-xl font-bold">Notifications</h1>
        <app-button variant="ghost" size="sm" (click)="markAllRead()">Mark all read</app-button>
      </div>

      <app-tabs [tabs]="tabs" [active]="activeTab" (activeChange)="setTab($event)" />

      <div class="mt-4 flex flex-col gap-2">
        @if (loading) {
          @for (i of [1,2,3,4]; track i) { <app-skeleton height="72px" /> }
        } @else if (filtered().length === 0) {
          <p class="text-center text-[var(--color-text-muted)] py-8 text-sm">
            {{ activeTab === 'unread' ? 'No unread notifications.' : 'No notifications.' }}
          </p>
        } @else {
          @for (n of filtered(); track n.id) {
            <div
              [class]="n.readAt ? 'opacity-60' : ''"
              class="flex items-start gap-4 bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div [class]="severityDot(n.severity)" class="mt-1 w-2.5 h-2.5 rounded-full flex-shrink-0"></div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium">{{ templateLabel(n.templateKey) }}</p>
                <p class="text-xs text-[var(--color-text-muted)] mt-0.5 truncate">{{ renderPayload(n.payload) }}</p>
                <p class="text-xs text-[var(--color-text-muted)] mt-1">{{ formatDate(n.createdAt) }}</p>
              </div>
              @if (!n.readAt) {
                <button (click)="markRead(n)"
                  class="text-xs text-[var(--color-brand-600)] hover:underline flex-shrink-0 min-h-0">
                  Mark read
                </button>
              }
            </div>
          }
        }
      </div>
    </div>
  `,
})
export class InboxComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  loading = false;
  activeTab = 'unread';
  tabs: Tab[] = [
    { id: 'unread', label: 'Unread' },
    { id: 'all', label: 'All' },
  ];

  private pollSub?: Subscription;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.load();
    // Poll for new notifications every 30 seconds
    this.pollSub = interval(30_000).pipe(
      switchMap(() => this.http.get<{ content: Notification[] }>('/api/v1/notifications?size=50').pipe(
        catchError(() => of({ content: [] }))
      ))
    ).subscribe(r => {
      this.notifications = r.content;
      // Update unread tab badge
      const unreadCount = r.content.filter(n => !n.readAt).length;
      this.tabs = [
        { id: 'unread', label: 'Unread', badge: unreadCount || undefined },
        { id: 'all', label: 'All', badge: r.content.length || undefined },
      ];
    });
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  load(): void {
    this.loading = true;
    this.http.get<{ content: Notification[] }>('/api/v1/notifications?size=50').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => {
      this.notifications = r.content;
      const unreadCount = r.content.filter(n => !n.readAt).length;
      this.tabs = [
        { id: 'unread', label: 'Unread', badge: unreadCount || undefined },
        { id: 'all', label: 'All', badge: r.content.length || undefined },
      ];
      this.loading = false;
    });
  }

  setTab(id: string): void { this.activeTab = id; }

  filtered(): Notification[] {
    return this.activeTab === 'unread'
      ? this.notifications.filter(n => !n.readAt)
      : this.notifications;
  }

  markRead(n: Notification): void {
    this.http.post(`/api/v1/notifications/${n.id}/read`, {}).pipe(catchError(() => of(null))).subscribe(() => {
      const idx = this.notifications.findIndex(x => x.id === n.id);
      if (idx >= 0) this.notifications[idx] = { ...n, readAt: new Date().toISOString() };
    });
  }

  markAllRead(): void {
    this.http.post('/api/v1/notifications/read-all', {}).pipe(catchError(() => of(null))).subscribe(() => {
      const now = new Date().toISOString();
      this.notifications = this.notifications.map(n => n.readAt ? n : { ...n, readAt: now });
    });
  }

  templateLabel(key: string): string {
    const map: Record<string, string> = {
      'export.ready': 'Export Ready',
      'export.failed': 'Export Failed',
      'anomaly.newDevice': 'New Device Login',
      'anomaly.ipOutOfRange': 'IP Out of Range',
      'anomaly.exportBurst': 'Export Burst Detected',
      'approval.requested': 'Approval Requested',
      'approval.decided': 'Approval Decision',
      'cert.expiring30': 'Certificate Expiring (30 days)',
      'cert.expiring60': 'Certificate Expiring (60 days)',
      'cert.expiring90': 'Certificate Expiring (90 days)',
    };
    return map[key] ?? key;
  }

  renderPayload(payloadJson: string): string {
    try {
      const obj = JSON.parse(payloadJson);
      return Object.entries(obj).map(([k, v]) => `${k}: ${v}`).join(', ');
    } catch {
      return payloadJson;
    }
  }

  severityDot(severity: string): string {
    const map: Record<string, string> = {
      INFO: 'bg-sky-400',
      WARN: 'bg-amber-400',
      ERROR: 'bg-red-500',
    };
    return map[severity] ?? 'bg-gray-400';
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
