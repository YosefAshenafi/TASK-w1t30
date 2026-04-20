import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

function passwordStrength(pwd: string): { score: number; label: string; color: string } {
  let score = 0;
  if (pwd.length >= 12) score++;
  if (/[A-Z]/.test(pwd)) score++;
  if (/[0-9]/.test(pwd)) score++;
  if (/[^A-Za-z0-9]/.test(pwd)) score++;
  const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
  const colors = ['', 'bg-red-500', 'bg-amber-400', 'bg-yellow-400', 'bg-green-500'];
  return { score, label: labels[score] ?? '', color: colors[score] ?? '' };
}

function matchPasswords(g: AbstractControl) {
  const pw = g.get('password')?.value;
  const confirm = g.get('confirmPassword')?.value;
  return pw && confirm && pw !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-[var(--color-surface-raised)] px-4 py-8">
      <div class="w-full max-w-sm bg-[var(--color-surface)] rounded-2xl shadow-lg p-8">
        <h1 class="text-2xl font-bold text-[var(--color-brand-600)] mb-1">Create account</h1>
        <p class="text-sm text-[var(--color-text-muted)] mb-6">Meridian Training Analytics</p>

        @if (errorMessage) {
          <div class="mb-4 px-3 py-2 rounded bg-red-50 border border-red-200 text-red-700 text-sm">
            {{ errorMessage }}
          </div>
        }

        <form [formGroup]="form" (ngSubmit)="submit()" class="flex flex-col gap-4">
          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium">Username</label>
            <input formControlName="username" type="text"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="username" />
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium">Display name</label>
            <input formControlName="displayName" type="text"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="Full name" />
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium">Role</label>
            <select formControlName="role"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]">
              <option value="">Select role…</option>
              <option value="STUDENT">Student</option>
              <option value="CORPORATE_MENTOR">Corporate Mentor</option>
              <option value="FACULTY_MENTOR">Faculty Mentor</option>
            </select>
          </div>

          @if (form.value.role === 'CORPORATE_MENTOR') {
            <div class="flex flex-col gap-1">
              <label class="text-sm font-medium">Organisation code</label>
              <input formControlName="orgCode" type="text"
                class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
                placeholder="ORG-xxxx" />
            </div>
          }

          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium">Password</label>
            <input formControlName="password" type="password"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="Min 12 chars, 1 digit, 1 symbol" />
            @if (form.value.password) {
              <div class="flex items-center gap-2 mt-1">
                <div class="flex-1 h-1.5 rounded bg-gray-200 overflow-hidden">
                  <div class="h-full rounded transition-all" [class]="strengthBar()" [style.width]="(strength.score * 25) + '%'"></div>
                </div>
                <span class="text-xs text-[var(--color-text-muted)]">{{ strength.label }}</span>
              </div>
            }
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-sm font-medium">Confirm password</label>
            <input formControlName="confirmPassword" type="password"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[var(--color-brand-500)] min-h-[48px]"
              placeholder="Repeat password" />
            @if (form.errors?.['mismatch'] && form.get('confirmPassword')?.touched) {
              <p class="text-xs text-red-600">Passwords do not match.</p>
            }
          </div>

          <button
            type="submit"
            [disabled]="loading || form.invalid"
            class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm font-medium hover:bg-[var(--color-brand-700)] disabled:opacity-50 min-h-[48px]">
            {{ loading ? 'Creating account…' : 'Create account' }}
          </button>
        </form>

        <p class="mt-4 text-center text-sm text-[var(--color-text-muted)]">
          Have an account? <a routerLink="/login" class="text-[var(--color-brand-600)] hover:underline">Sign in</a>
        </p>
      </div>
    </div>
  `,
})
export class RegisterComponent {
  form: FormGroup;
  loading = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
  ) {
    this.form = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      displayName: ['', Validators.required],
      role: ['', Validators.required],
      orgCode: [''],
      password: ['', [Validators.required, Validators.minLength(12)]],
      confirmPassword: ['', Validators.required],
    }, { validators: matchPasswords });
  }

  get strength() {
    return passwordStrength(this.form.value.password ?? '');
  }

  strengthBar(): string {
    return this.strength.color;
  }

  submit(): void {
    if (this.form.invalid || this.loading) return;
    this.loading = true;
    this.errorMessage = '';

    const { username, displayName, role, orgCode, password } = this.form.value;
    const body: Record<string, unknown> = { username, displayName, requestedRole: role, password };
    if (role === 'CORPORATE_MENTOR' && orgCode) {
      body['organizationCode'] = orgCode;
    }

    this.http.post('/api/v1/auth/register', body).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: 1 } }),
      error: err => {
        this.loading = false;
        this.errorMessage = err.error?.message ?? 'Registration failed. Please try again.';
      },
    });
  }
}
