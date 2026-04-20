import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { catchError, of } from 'rxjs';

interface Anomaly {
  id: string;
  userId: string;
  type: string;
  ipAddress: string;
  details: string;
  occurredAt: string;
  resolvedAt: string | null;
}

@Component({
  selector: 'app-admin-anomalies',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Security Anomalies</h1>

      @if (message) {
        <app-banner [message]="message" severity="success" [dismissible]="true" (dismissed)="message=''" />
      }

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3]; track i) { <app-skeleton height="72px" /> }
        </div>
      } @else if (items.length === 0) {
        <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">No unresolved anomalies.</p>
      } @else {
        <div class="flex flex-col gap-3">
          @for (item of items; track item.id) {
            <div class="bg-red-50 border border-red-200 rounded-xl px-5 py-4">
              <div class="flex items-start justify-between gap-4">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="text-xs font-bold px-2 py-1 rounded-full bg-red-100 text-red-800">{{ item.type }}</span>
                    <span class="text-xs text-[var(--color-text-muted)] font-mono">{{ item.ipAddress }}</span>
                  </div>
                  <p class="text-xs text-[var(--color-text-muted)] mt-1.5">
                    User: {{ item.userId?.slice(-8) }} · {{ formatDate(item.occurredAt) }}
                  </p>
                </div>
                @if (!item.resolvedAt) {
                  <app-button variant="secondary" size="sm" (click)="resolve(item)">Resolve</app-button>
                } @else {
                  <span class="text-xs text-green-700">Resolved</span>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
})
export class AdminAnomaliesComponent implements OnInit {
  items: Anomaly[] = [];
  loading = false;
  message = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<{ content: Anomaly[] }>('/api/v1/admin/anomalies?resolved=false&size=50').pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.items = r.content; this.loading = false; });
  }

  resolve(item: Anomaly): void {
    this.http.post(`/api/v1/admin/anomalies/${item.id}/resolve`, {}).pipe(catchError(() => of(null))).subscribe(ok => {
      if (ok !== null) { this.message = 'Anomaly resolved.'; this.load(); }
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
