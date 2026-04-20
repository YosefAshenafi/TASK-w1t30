import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthStore } from '../../core/stores/auth.store';
import { NetworkStatusService } from '../../core/stores/network-status.service';
import { ToastContainerComponent } from './toast-container.component';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface NavItem {
  label: string;
  path: string;
  roles?: string[];
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Home', path: '/home', icon: '⌂' },
  { label: 'Sessions', path: '/sessions', roles: ['STUDENT'], icon: '▶' },
  { label: 'Analytics', path: '/analytics/mastery', roles: ['CORPORATE_MENTOR', 'FACULTY_MENTOR', 'ADMIN'], icon: '◈' },
  { label: 'Reports', path: '/reports', roles: ['CORPORATE_MENTOR', 'FACULTY_MENTOR', 'ADMIN'], icon: '⊞' },
  { label: 'Notifications', path: '/notifications', icon: '🔔' },
  { label: 'Admin', path: '/admin/users', roles: ['ADMIN'], icon: '⚙' },
];

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ToastContainerComponent],
  template: `
    <div class="flex h-screen overflow-hidden bg-[var(--color-surface)]">
      <!-- Sidebar -->
      <aside
        class="flex flex-col shrink-0 bg-[var(--color-surface-raised)] border-r border-[var(--color-border)] transition-all"
        [class.w-56]="!collapsed()"
        [class.w-14]="collapsed()">

        <!-- Logo / toggle -->
        <div class="flex items-center justify-between px-3 h-14 border-b border-[var(--color-border)]">
          @if (!collapsed()) {
            <span class="font-bold text-[var(--color-brand-600)] truncate">Meridian</span>
          }
          <button
            (click)="collapsed.set(!collapsed())"
            class="w-8 h-8 flex items-center justify-center rounded hover:bg-[var(--color-surface-overlay)] min-h-0 text-xs">
            {{ collapsed() ? '›' : '‹' }}
          </button>
        </div>

        <!-- Offline banner -->
        @if (!(networkStatus.online$ | async)) {
          <div class="px-2 py-1 bg-amber-100 text-amber-800 text-xs text-center">
            {{ collapsed() ? '!' : 'Offline' }}
          </div>
        }

        <!-- Nav items -->
        <nav class="flex-1 flex flex-col gap-1 py-2 overflow-y-auto">
          @for (item of visibleItems(); track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="bg-[var(--color-brand-100)] text-[var(--color-brand-700)] font-semibold"
              class="flex items-center gap-3 px-3 py-2 mx-1 rounded text-sm text-[var(--color-text)] hover:bg-[var(--color-surface-overlay)] min-h-0 h-10">
              <span class="text-base w-5 text-center flex-shrink-0">{{ item.icon }}</span>
              @if (!collapsed()) {
                <span class="truncate">{{ item.label }}</span>
              }
            </a>
          }
        </nav>

        <!-- User info + logout -->
        <div class="border-t border-[var(--color-border)] px-3 py-2">
          @if (!collapsed()) {
            <div class="text-xs text-[var(--color-text-muted)] truncate mb-1">
              {{ (authStore.profile$ | async)?.username }}
            </div>
          }
          <button
            (click)="logout()"
            class="w-full text-left text-xs text-[var(--color-text-muted)] hover:text-[var(--color-danger)] min-h-0 h-8">
            {{ collapsed() ? '↩' : 'Sign out' }}
          </button>
        </div>
      </aside>

      <!-- Main content -->
      <main class="flex-1 overflow-auto">
        <router-outlet />
      </main>
    </div>

    <app-toast-container />
  `,
})
export class AppShellComponent {
  collapsed = signal(false);

  constructor(
    readonly authStore: AuthStore,
    readonly networkStatus: NetworkStatusService,
    private router: Router,
    private http: HttpClient,
  ) {}

  visibleItems(): NavItem[] {
    const role = this.authStore.userRole();
    return NAV_ITEMS.filter(item =>
      !item.roles || (role && item.roles.includes(role))
    );
  }

  logout(): void {
    const rt = this.authStore.refreshToken();
    this.http.post('/api/v1/auth/logout', rt ? { refreshToken: rt } : {}).subscribe({
      complete: () => {
        this.authStore.clearProfile();
        this.router.navigate(['/login']);
      },
      error: () => {
        this.authStore.clearProfile();
        this.router.navigate(['/login']);
      },
    });
  }
}
