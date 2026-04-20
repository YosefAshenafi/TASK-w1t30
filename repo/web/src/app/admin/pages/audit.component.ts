import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { catchError, of } from 'rxjs';

interface AuditEvent {
  id: string;
  actorId: string;
  action: string;
  targetType: string;
  targetId: string;
  ipAddress: string;
  details: string;
  occurredAt: string;
}

@Component({
  selector: 'app-admin-audit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SkeletonComponent],
  template: `
    <div class="p-6 max-w-6xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Audit Log</h1>

      <form [formGroup]="filters" class="flex flex-wrap gap-3 mb-6">
        <input formControlName="action" type="text" placeholder="Action (e.g. LOGIN)"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="actorId" type="text" placeholder="Actor UUID"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="from" type="date"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <input formControlName="to" type="date"
          class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        <button type="button" (click)="load()"
          class="bg-[var(--color-brand-600)] text-white rounded-lg px-4 py-2 text-sm min-h-[48px]">
          Search
        </button>
      </form>

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3,4,5]; track i) { <app-skeleton height="48px" /> }
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
          <table class="w-full text-sm">
            <thead class="bg-[var(--color-surface-raised)]">
              <tr>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Time</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Action</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Target</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">IP</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Actor</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (event of events; track event.id) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3 text-xs text-[var(--color-text-muted)]">{{ formatDate(event.occurredAt) }}</td>
                  <td class="px-4 py-3 font-mono text-xs font-medium">{{ event.action }}</td>
                  <td class="px-4 py-3 text-xs">{{ event.targetType }}/{{ event.targetId }}</td>
                  <td class="px-4 py-3 font-mono text-xs">{{ event.ipAddress }}</td>
                  <td class="px-4 py-3 font-mono text-xs">{{ event.actorId?.slice(-8) }}</td>
                </tr>
              }
              @if (events.length === 0) {
                <tr><td colspan="5" class="px-4 py-8 text-center text-[var(--color-text-muted)]">No events found.</td></tr>
              }
            </tbody>
          </table>
        </div>
        @if (totalElements > events.length) {
          <p class="text-xs text-[var(--color-text-muted)] mt-2 text-right">
            Showing {{ events.length }} of {{ totalElements }}
          </p>
        }
      }
    </div>
  `,
})
export class AdminAuditComponent implements OnInit {
  filters: FormGroup;
  events: AuditEvent[] = [];
  loading = false;
  totalElements = 0;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.filters = this.fb.group({ action: [''], actorId: [''], from: [''], to: [''] });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const { action, actorId, from, to } = this.filters.value;
    let params = 'size=100';
    if (action) params += `&action=${action}`;
    if (actorId) params += `&actorId=${actorId}`;
    if (from) params += `&from=${from}`;
    if (to) params += `&to=${to}`;

    this.http.get<{ content: AuditEvent[]; totalElements: number }>(`/api/v1/admin/audit?${params}`).pipe(
      catchError(() => of({ content: [], totalElements: 0 }))
    ).subscribe(r => {
      this.events = r.content;
      this.totalElements = r.totalElements;
      this.loading = false;
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleString();
  }
}
