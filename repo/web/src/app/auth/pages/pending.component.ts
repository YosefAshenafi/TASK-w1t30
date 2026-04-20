import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { UserProfile } from '../../core/models/user.model';
import { interval, Subscription } from 'rxjs';
import { switchMap, catchError, of } from 'rxjs';

@Component({
  selector: 'app-pending',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-[var(--color-surface-raised)] px-4">
      <div class="w-full max-w-sm bg-[var(--color-surface)] rounded-2xl shadow-lg p-8 text-center">
        <div class="text-4xl mb-4">⏳</div>
        <h1 class="text-xl font-bold mb-2">Account Pending Approval</h1>
        <p class="text-sm text-[var(--color-text-muted)] mb-4">
          Your account is awaiting administrator review (SLA: 2 business days).
          You'll be redirected automatically when approved.
        </p>
        <p class="text-xs text-[var(--color-text-muted)]">
          Checking status automatically…
        </p>
        <button
          (click)="logout()"
          class="mt-6 text-sm text-[var(--color-text-muted)] hover:text-[var(--color-danger)] underline min-h-0">
          Sign out
        </button>
      </div>
    </div>
  `,
})
export class PendingComponent implements OnInit, OnDestroy {
  private pollSubscription?: Subscription;

  constructor(
    private http: HttpClient,
    private authStore: AuthStore,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.pollSubscription = interval(60_000).pipe(
      switchMap(() =>
        this.http.get<UserProfile>('/api/v1/users/me').pipe(
          catchError(() => of(null))
        )
      )
    ).subscribe(profile => {
      if (!profile) return;
      this.authStore.setProfile(profile);
      if (profile.status === 'ACTIVE') {
        this.router.navigate(['/home']);
      } else if (profile.status === 'SUSPENDED') {
        this.authStore.clearProfile();
        this.router.navigate(['/login']);
      }
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
  }

  logout(): void {
    this.authStore.clearProfile();
    this.router.navigate(['/login']);
  }
}
