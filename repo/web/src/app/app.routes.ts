import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './auth/pages/login.component';
import { RegisterComponent } from './auth/pages/register.component';
import { PendingComponent } from './auth/pages/pending.component';
import { HomeComponent } from './home/home.component';
import { AppShellComponent } from './shared/ui/app-shell.component';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // Public auth pages (no shell) — /pending stays outside the authenticated
  // shell so PENDING-status users (whose login is rejected with 403) can
  // still see the pending-state UI without triggering the auth guard.
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'pending', component: PendingComponent },

  // Authenticated shell wrapper
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'home', component: HomeComponent },

      // Sessions — Student only
      {
        path: 'sessions',
        canActivate: [roleGuard],
        data: { roles: ['STUDENT'] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./sessions/pages/sessions-list.component').then(m => m.SessionsListComponent),
          },
          {
            path: 'new',
            loadComponent: () =>
              import('./sessions/pages/session-new.component').then(m => m.SessionNewComponent),
          },
          {
            path: ':id/run',
            loadComponent: () =>
              import('./sessions/pages/session-run.component').then(m => m.SessionRunComponent),
          },
        ],
      },

      // Analytics — Mentor + Admin
      {
        path: 'analytics',
        canActivate: [roleGuard],
        data: { roles: ['CORPORATE_MENTOR', 'FACULTY_MENTOR', 'ADMIN'] },
        children: [
          {
            path: 'mastery',
            loadComponent: () =>
              import('./analytics/pages/mastery-trends.component').then(m => m.MasteryTrendsComponent),
          },
          {
            path: 'items',
            loadComponent: () =>
              import('./analytics/pages/item-stats.component').then(m => m.ItemStatsComponent),
          },
          {
            path: 'weak-points',
            loadComponent: () =>
              import('./analytics/pages/weak-knowledge-points.component').then(
                m => m.WeakKnowledgePointsComponent
              ),
          },
          {
            path: 'wrong-answers',
            loadComponent: () =>
              import('./analytics/pages/wrong-answers.component').then(m => m.WrongAnswersComponent),
          },
        ],
      },

      // Reports — Mentor + Admin
      {
        path: 'reports',
        canActivate: [roleGuard],
        data: { roles: ['CORPORATE_MENTOR', 'FACULTY_MENTOR', 'ADMIN'] },
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./reports/pages/reports-center.component').then(m => m.ReportsCenterComponent),
          },
          {
            path: 'schedules',
            loadComponent: () =>
              import('./reports/pages/schedules.component').then(m => m.SchedulesComponent),
          },
        ],
      },

      // Notifications — all authenticated
      {
        path: 'notifications',
        loadComponent: () =>
          import('./notifications/pages/inbox.component').then(m => m.InboxComponent),
      },

      // Admin — Admin only
      {
        path: 'admin',
        canActivate: [roleGuard],
        data: { roles: ['ADMIN'] },
        children: [
          {
            path: 'users',
            loadComponent: () =>
              import('./admin/pages/users.component').then(m => m.AdminUsersComponent),
          },
          {
            path: 'approvals',
            loadComponent: () =>
              import('./admin/pages/approvals.component').then(m => m.AdminApprovalsComponent),
          },
          {
            path: 'audit',
            loadComponent: () =>
              import('./admin/pages/audit.component').then(m => m.AdminAuditComponent),
          },
          {
            path: 'anomalies',
            loadComponent: () =>
              import('./admin/pages/anomalies.component').then(m => m.AdminAnomaliesComponent),
          },
          {
            path: 'templates',
            loadComponent: () =>
              import('./admin/pages/templates.component').then(m => m.AdminTemplatesComponent),
          },
          {
            path: 'ip-ranges',
            loadComponent: () =>
              import('./admin/pages/ip-ranges.component').then(m => m.AdminIpRangesComponent),
          },
          {
            path: 'recycle-bin',
            loadComponent: () =>
              import('./admin/pages/recycle-bin.component').then(m => m.AdminRecycleBinComponent),
          },
          {
            path: 'backups',
            loadComponent: () =>
              import('./admin/pages/backups.component').then(m => m.AdminBackupsComponent),
          },
        ],
      },
    ],
  },

  { path: '**', redirectTo: '/home' },
];
