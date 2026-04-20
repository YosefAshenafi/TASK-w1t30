import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { TabsComponent, Tab } from '../../shared/ui/tabs.component';
import { catchError, of } from 'rxjs';

interface RecycleBinEntry {
  id: string;
  type: string;
  label: string;
  deletedAt: string;
}

@Component({
  selector: 'app-admin-recycle-bin',
  standalone: true,
  imports: [CommonModule, ButtonComponent, SkeletonComponent, BannerComponent, TabsComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Recycle Bin</h1>

      @if (message) {
        <app-banner [message]="message" severity="success" [dismissible]="true" (dismissed)="message=''" />
      }

      <app-tabs [tabs]="tabs" [active]="activeTab" (activeChange)="setTab($event)" />

      <div class="mt-4">
        @if (loading) {
          <div class="flex flex-col gap-3">
            @for (i of [1,2,3]; track i) { <app-skeleton height="56px" /> }
          </div>
        } @else if (entries.length === 0) {
          <p class="text-[var(--color-text-muted)] text-sm py-8 text-center">Recycle bin is empty.</p>
        } @else {
          <div class="flex flex-col gap-2">
            @for (entry of entries; track entry.id) {
              <div class="flex items-center justify-between bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl px-5 py-4">
                <div>
                  <p class="font-medium text-sm">{{ entry.label }}</p>
                  <p class="text-xs text-[var(--color-text-muted)] mt-0.5">Deleted {{ formatDate(entry.deletedAt) }}</p>
                </div>
                <div class="flex gap-2">
                  <app-button variant="secondary" size="sm" (click)="restore(entry)">Restore</app-button>
                  <app-button variant="danger" size="sm" (click)="hardDelete(entry)">Delete permanently</app-button>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </div>
  `,
})
export class AdminRecycleBinComponent implements OnInit {
  entries: RecycleBinEntry[] = [];
  loading = false;
  message = '';
  activeTab = 'courses';
  tabs: Tab[] = [
    { id: 'courses', label: 'Courses' },
    { id: 'users', label: 'Users' },
  ];

  constructor(private http: HttpClient) {}

  ngOnInit(): void { this.load(); }

  setTab(id: string): void { this.activeTab = id; this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<{ content: RecycleBinEntry[] }>(`/api/v1/admin/recycle-bin?type=${this.activeTab}`).pipe(
      catchError(() => of({ content: [] }))
    ).subscribe(r => { this.entries = r.content; this.loading = false; });
  }

  restore(entry: RecycleBinEntry): void {
    this.http.post(`/api/v1/admin/recycle-bin/${entry.type}/${entry.id}/restore`, {}).pipe(
      catchError(() => of(null))
    ).subscribe(ok => {
      if (ok !== null) { this.message = `${entry.label} restored.`; this.load(); }
    });
  }

  hardDelete(entry: RecycleBinEntry): void {
    if (!confirm(`Permanently delete "${entry.label}"? This cannot be undone.`)) return;
    this.http.delete(`/api/v1/admin/recycle-bin/${entry.type}/${entry.id}`).pipe(
      catchError(() => of(null))
    ).subscribe(() => {
      this.message = `${entry.label} permanently deleted.`;
      this.entries = this.entries.filter(e => e.id !== entry.id);
    });
  }

  formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString();
  }
}
