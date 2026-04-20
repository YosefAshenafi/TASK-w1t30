import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { TabsComponent, Tab } from '../../shared/ui/tabs.component';
import { catchError, of } from 'rxjs';

interface User {
  id: string;
  username: string;
  displayName: string;
  role: string;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
}

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent, BannerComponent, TabsComponent],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <h1 class="text-xl font-bold mb-4">User Management</h1>

      @if (message) {
        <app-banner [message]="message" [severity]="messageType" [dismissible]="true" (dismissed)="message=''" />
      }

      <app-tabs [tabs]="tabs" [active]="activeTab" (activeChange)="setTab($event)" />

      <div class="mt-4">
        @if (loading) {
          <div class="flex flex-col gap-3">
            @for (i of [1,2,3,4]; track i) { <app-skeleton height="56px" /> }
          </div>
        } @else {
          <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
            <table class="w-full text-sm">
              <thead class="bg-[var(--color-surface-raised)]">
                <tr>
                  <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Username</th>
                  <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Role</th>
                  <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Status</th>
                  <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Last login</th>
                  <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--color-border)]">
                @for (user of users; track user.id) {
                  <tr class="hover:bg-[var(--color-surface-raised)]">
                    <td class="px-4 py-3 font-medium">{{ user.username }}</td>
                    <td class="px-4 py-3 text-[var(--color-text-muted)]">{{ user.role }}</td>
                    <td class="px-4 py-3">
                      <span [class]="statusClass(user.status)" class="text-xs px-2 py-1 rounded-full font-medium">
                        {{ user.status }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-[var(--color-text-muted)] text-xs">
                      {{ user.lastLoginAt ? formatDate(user.lastLoginAt) : 'Never' }}
                    </td>
                    <td class="px-4 py-3 text-right">
                      <div class="flex justify-end gap-2">
                        @if (user.status === 'PENDING') {
                          <app-button variant="primary" size="sm" (click)="approve(user)">Approve</app-button>
                          <app-button variant="danger" size="sm" (click)="reject(user)">Reject</app-button>
                        }
                        @if (user.status === 'LOCKED') {
                          <app-button variant="secondary" size="sm" (click)="unlock(user)">Unlock</app-button>
                        }
                      </div>
                    </td>
                  </tr>
                }
                @if (users.length === 0) {
                  <tr><td colspan="5" class="px-4 py-8 text-center text-[var(--color-text-muted)]">No users found.</td></tr>
                }
              </tbody>
            </table>
          </div>
        }
      </div>
    </div>
  `,
})
export class AdminUsersComponent implements OnInit {
  users: User[] = [];
  loading = false;
  message = '';
  messageType: 'success' | 'error' = 'success';
  activeTab = 'PENDING';
  tabs: Tab[] = [
    { id: 'PENDING', label: 'Pending' },
    { id: 'ACTIVE', label: 'Active' },
    { id: 'LOCKED', label: 'Locked' },
    { id: 'SUSPENDED', label: 'Suspended' },
    { id: '', label: 'All' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  setTab(id: string): void { this.activeTab = id; this.load(); }

  load(): void {
    this.loading = true;
    const params = this.activeTab ? `?status=${this.activeTab}&size=100` : '?size=100';
    this.http.get<{ content: User[] }>(`/api/v1/admin/users${params}`).pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.users = r.content; this.loading = false; });
  }

  approve(user: User): void {
    this.http.post(`/api/v1/admin/users/${user.id}/approve`, {}).pipe(catchError(() => of(null))).subscribe(ok => {
      if (ok !== null) { this.message = `${user.username} approved.`; this.messageType = 'success'; this.load(); }
      else { this.message = 'Action failed.'; this.messageType = 'error'; }
    });
  }

  reject(user: User): void {
    const reason = window.prompt(`Reason for rejecting ${user.username}:`);
    if (!reason || !reason.trim()) return;
    this.http.post(`/api/v1/admin/users/${user.id}/reject`, { reason: reason.trim() }).pipe(catchError(() => of(null))).subscribe(ok => {
      if (ok !== null) { this.message = `${user.username} rejected.`; this.messageType = 'success'; this.load(); }
      else { this.message = 'Action failed.'; this.messageType = 'error'; }
    });
  }

  unlock(user: User): void {
    this.http.post(`/api/v1/admin/users/${user.id}/unlock`, {}).pipe(catchError(() => of(null))).subscribe(ok => {
      if (ok !== null) { this.message = `${user.username} unlocked.`; this.messageType = 'success'; this.load(); }
      else { this.message = 'Action failed.'; this.messageType = 'error'; }
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'bg-amber-100 text-amber-800',
      ACTIVE: 'bg-green-100 text-green-800',
      LOCKED: 'bg-red-100 text-red-800',
      SUSPENDED: 'bg-gray-100 text-gray-600',
    };
    return map[status] ?? 'bg-gray-100 text-gray-600';
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString();
  }
}
