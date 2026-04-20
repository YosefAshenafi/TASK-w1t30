import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthStore } from '../../core/stores/auth.store';
import { UserProfile } from '../../core/models/user.model';

async function computeFingerprint(): Promise<string> {
  const raw = [
    navigator.userAgent,
    navigator.language,
    screen.width + 'x' + screen.height,
    Intl.DateTimeFormat().resolvedOptions().timeZone,
    navigator.hardwareConcurrency ?? 0,
  ].join('|');
  const buf = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(raw));
  return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2, '0')).join('');
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-[var(--color-surface-raised)] px-4">
      <div class="w-full max-w-sm bg-[var(--color-surface)] rounded-2xl shadow-lg p-8">
        <h1 class="text-2xl font-bold text-[var(--color-brand-600)] mb-1">Meridian</h1>
        <p class="text-sm text-[var(--color-text-muted)] mb-6">Training Analytics Management</p>

        @if (errorMessage) {
          <div class="mb-4 px-3 py-2 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
            {{ errorMessage }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium text-[var(--color-text)]">Username</label>
            <input
              formControlName="username"
              type="text"
              autocomplete="username"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="username" />
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium text-[var(--color-text)]">Password</label>
            <input
              formControlName="password"
              type="password"
              autocomplete="current-password"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="••••••••" />
          </div>

          <button
            type="submit"
            [disabled]="loading || form.invalid"
            class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm font-medium hover:bg-[var(--color-brand-700)] disabled:opacity-50 min-h-[48px]">
            {{ loading ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-[var(--color-text-muted)]">
          No account? <a routerLink="/register" class="text-[var(--color-brand-600)] hover:underline">Register</a>
        </p>
      </div>
    </div>
  `,
})
export class LoginComponent implements OnInit {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  private returnUrl = '/home';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private authStore: AuthStore,
    private router: Router,
    private route: ActivatedRoute,
  ) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/home';
    if (this.authStore.isAuthenticated()) {
      this.router.navigate([this.returnUrl]);
    }
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.errorMessage = '';

    const fingerprint = await computeFingerprint();
    const { username, password } = this.form.value;

    this.http.post<{ accessToken: string; refreshToken: string; user: UserProfile }>(
      '/api/v1/auth/login',
      { username, password, deviceFingerprint: fingerprint }
    ).subscribe({
      next: res => {
        this.authStore.login(res.accessToken, res.refreshToken, res.user);
        const status = res.user.status;
        if (status === 'PENDING') {
          this.router.navigate(['/pending']);
        } else {
          this.router.navigate([this.returnUrl]);
        }
      },
      error: err => {
        this.loading = false;
        const code = err.error?.code;
        if (code === 'ACCOUNT_LOCKED') {
          this.errorMessage = 'Account locked after too many failed attempts. Try again in 15 minutes.';
        } else if (code === 'ACCOUNT_SUSPENDED') {
          this.errorMessage = 'Account suspended. Contact your administrator.';
        } else if (code === 'ACCOUNT_PENDING') {
          this.router.navigate(['/pending']);
        } else if (err.status === 401) {
          this.errorMessage = 'Invalid username or password.';
        } else {
          this.errorMessage = err.error?.message ?? 'Login failed. Please try again.';
        }
      },
    });
  }
}
