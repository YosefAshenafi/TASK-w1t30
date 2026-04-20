import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { catchError, of } from 'rxjs';

interface Approval {
  id: string;
  type: string;
  status: string;
  requestedBy: string;
  reviewedBy: string | null;
  reason: string | null;
  createdAt: string;
  decidedAt: string | null;
  expiresAt: string;
}

@Component({
  selector: 'app-admin-approvals',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Pending Approvals</h1>

      @if (message) {
        <app-banner [message]="message" severity="success" [dismissible]="true" (dismissed)="message=''" />
      }

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3]; track i) { <app-skeleton height="72px" /> }
        </div>
      } @else if (items.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">No pending approvals.</p>
      } @else {
        <div class="flex flex-col gap-3">
          @for (item of items; track item.id) {
            <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <p class="font-medium text-sm">{{ item.type }} · <span class="text-[var(--color-text-muted)]">{{ item.status }}</span></p>
                  @if (item.reason) {
                    <p class="text-xs text-[var(--color-text-muted)] mt-0.5">{{ item.reason }}</p>
                  }
                  <p class="text-xs text-[var(--color-text-muted)] mt-1">
                    Requested {{ formatDate(item.createdAt) }} · Expires {{ formatDate(item.expiresAt) }}
                  </p>
                </div>
                <div class="flex gap-2 flex-shrink-0">
                  <app-button variant="primary" size="sm" (click)="decide(item, 'APPROVED')">Approve</app-button>
                  <app-button variant="danger" size="sm" (click)="decide(item, 'REJECTED')">Reject</app-button>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class AdminApprovalsComponent implements OnInit {
  items: Approval[] = [];
  loading = false;
  message = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<{ content: Approval[] }>('/api/v1/admin/approvals?status=PENDING&size=50').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.items = r.content; this.loading = false; });
  }

  decide(item: Approval, decision: string): void {
    const body = decision === 'REJECTED' ? { reason: 'Rejected by admin' } : {};
    const path = decision === 'APPROVED' ? 'approve' : 'reject';
    this.http.post(`/api/v1/admin/approvals/${item.id}/${path}`, body).pipe(
      catchError(() => of(null))
    ).subscribe(ok => {
      if (ok !== null) { this.message = `Request ${decision.toLowerCase()}.`; this.load(); }
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
