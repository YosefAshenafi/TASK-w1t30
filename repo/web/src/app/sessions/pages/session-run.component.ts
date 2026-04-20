import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { interval, Subscription } from 'rxjs';
import { SessionStore } from '../session.store';
import { NetworkStatusService } from '../../core/stores/network-status.service';
import { BannerComponent } from '../../shared/ui/banner.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SessionRecord, SessionSetRecord } from '../../core/db/dexie';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-session-run',
  standalone: true,
  imports: [CommonModule, RouterLink, BannerComponent, ButtonComponent],
  template: `
    <div class="p-6 max-w-2xl mx-auto">
      @if (!(network.online$ | async)) {
        <app-banner message="Offline — progress saved locally." severity="warn" />
      }

      @if (session) {
        <div class="flex items-center justify-between mb-4">
          <div>
            <h1 class="text-xl font-bold">Session in progress</h1>
            <p class="text-xs text-[var(--color-text-muted)] mt-0.5">{{ session.id.slice(-8) }}</p>
          </div>
          <div class="text-right">
            <div class="font-mono text-2xl font-bold text-[var(--color-brand-600)]">
              {{ formatElapsed(elapsedSeconds) }}
            </div>
            <p class="text-xs text-[var(--color-text-muted)]">Elapsed</p>
          </div>
        </div>

        @if (resting && restRemaining > 0) {
          <div class="bg-amber-50 border border-amber-200 rounded-xl p-5 mb-4 text-center">
            <p class="text-sm font-medium text-amber-800 mb-1">Rest period</p>
            <div class="font-mono text-4xl font-bold text-amber-700">{{ restRemaining }}s</div>
          </div>
        }

        <!-- Activity sets -->
        <div class="flex flex-col gap-3 mb-6">
          @for (set of sets; track set.id) {
            <div
              [class]="set.completedAt ? 'border-green-300 bg-green-50' : 'border-[var(--color-border)] bg-[var(--color-surface)]'"
              class="flex items-center justify-between border rounded-xl px-5 py-4">
              <div>
                <p class="font-medium text-sm">Set {{ set.setIndex + 1 }}</p>
                <p class="text-xs text-[var(--color-text-muted)] mt-0.5">
                  Rest: {{ set.restSeconds }}s
                  @if (set.completedAt) { · ✓ {{ formatTime(set.completedAt) }} }
                </p>
              </div>
              @if (!set.completedAt) {
                <app-button variant="primary" size="sm" (click)="completeSet(set)">
                  Done
                </app-button>
              } @else {
                <span class="text-green-700 text-sm">✓</span>
              }
            </div>
          }
          <button
            (click)="addSet()"
            class="border-2 border-dashed border-[var(--color-border)] rounded-xl px-5 py-4 text-sm text-[var(--color-text-muted)] hover:border-[var(--color-brand-500)] hover:text-[var(--color-brand-600)] transition-colors min-h-0">
            + Add set
          </button>
        </div>

        <!-- Session controls -->
        <div class="flex gap-3">
          @if (session.status === 'IN_PROGRESS') {
            <app-button variant="secondary" (click)="pauseSession()">Pause</app-button>
          }
          @if (session.status === 'PAUSED') {
            <app-button variant="primary" (click)="resumeSession()">Resume</app-button>
          }
          <app-button variant="danger" (click)="completeSession()">Complete</app-button>
          <a routerLink="/sessions">
            <app-button variant="ghost">Back</app-button>
          </a>
        </div>
      } @else {
        <div class="text-center py-12 text-[var(--color-text-muted)]">Loading session…</div>
      }
    </div>
  `,
})
export class SessionRunComponent implements OnInit, OnDestroy {
  session: SessionRecord | null = null;
  sets: SessionSetRecord[] = [];
  elapsedSeconds = 0;
  restRemaining = 0;
  resting = false;

  private timerSub?: Subscription;
  private restSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private sessionStore: SessionStore,
    readonly network: NetworkStatusService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadSession(id);
    this.timerSub = interval(1000).subscribe(() => this.elapsedSeconds++);
  }

  ngOnDestroy(): void {
    this.timerSub?.unsubscribe();
    this.restSub?.unsubscribe();
  }

  private loadSession(id: string): void {
    this.http.get<SessionRecord>(`/api/v1/sessions/${id}`).pipe(
      catchError(() => of(null))
    ).subscribe(async s => {
      if (s) {
        this.session = s;
        await this.sessionStore.upsertSession(s);
      } else {
        // Try from cache
        const cached = await import('../../core/db/dexie').then(m => m.db.sessions.get(id));
        this.session = cached ?? null;
      }
      if (this.session) {
        this.sets = await this.sessionStore.getSetsForSession(id);
        if (this.session.startedAt) {
          this.elapsedSeconds = Math.floor(
            (Date.now() - new Date(this.session.startedAt).getTime()) / 1000
          );
        }
      }
    });
  }

  async completeSet(set: SessionSetRecord): Promise<void> {
    const now = new Date().toISOString();
    const updated: SessionSetRecord = { ...set, completedAt: now, clientUpdatedAt: now };
    await this.sessionStore.upsertSet(updated);
    const idx = this.sets.findIndex(s => s.id === set.id);
    if (idx >= 0) this.sets[idx] = updated;
    this.startRest(set.restSeconds);
  }

  async addSet(): Promise<void> {
    if (!this.session) return;
    const setId = crypto.randomUUID();
    const now = new Date().toISOString();
    const newSet: SessionSetRecord = {
      id: setId,
      sessionId: this.session.id,
      activityId: '',
      setIndex: this.sets.length,
      restSeconds: this.session.restSecondsDefault,
      completedAt: null,
      notes: null,
      clientUpdatedAt: now,
    };
    await this.sessionStore.upsertSet(newSet);
    this.sets = [...this.sets, newSet];
  }

  async pauseSession(): Promise<void> {
    if (!this.session) return;
    const now = new Date().toISOString();
    const updated = { ...this.session, status: 'PAUSED' as const, clientUpdatedAt: now };
    await this.sessionStore.upsertSession(updated);
    this.session = updated;
    this.http.post(`/api/v1/sessions/${this.session.id}/pause`, {}).pipe(catchError(() => of(null))).subscribe();
  }

  async resumeSession(): Promise<void> {
    if (!this.session) return;
    const now = new Date().toISOString();
    const updated = { ...this.session, status: 'IN_PROGRESS' as const, clientUpdatedAt: now };
    await this.sessionStore.upsertSession(updated);
    this.session = updated;
    this.http.post(`/api/v1/sessions/${this.session.id}/continue`, {}).pipe(catchError(() => of(null))).subscribe();
  }

  async completeSession(): Promise<void> {
    if (!this.session) return;
    const now = new Date().toISOString();
    const updated = { ...this.session, status: 'COMPLETED' as const, endedAt: now, clientUpdatedAt: now };
    await this.sessionStore.upsertSession(updated);
    this.session = updated;
    this.http.patch(`/api/v1/sessions/${this.session.id}`, { status: 'COMPLETED', endedAt: now, clientUpdatedAt: now })
      .pipe(catchError(() => of(null)))
      .subscribe(() => this.router.navigate(['/sessions']));
  }

  private startRest(seconds: number): void {
    if (seconds <= 0) return;
    this.resting = true;
    this.restRemaining = seconds;
    this.restSub?.unsubscribe();
    this.restSub = interval(1000).subscribe(() => {
      this.restRemaining--;
      if (this.restRemaining <= 0) {
        this.resting = false;
        this.restSub?.unsubscribe();
      }
    });
  }

  formatElapsed(secs: number): string {
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  formatTime(iso: string | null): string {
    if (!iso) return '';
    return new Date(iso).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
  }
}
