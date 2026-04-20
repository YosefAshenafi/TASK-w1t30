import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { catchError, of } from 'rxjs';

interface IpRange {
  id: string;
  cidr: string;
  roleScope: string | null;
  note: string | null;
}

@Component({
  selector: 'app-admin-ip-ranges',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SkeletonComponent, BannerComponent],
  template: `
    <div class="p-6 max-w-4xl mx-auto">
      <h1 class="text-xl font-bold mb-4">IP Allow-list</h1>

      @if (message) {
        <app-banner [message]="message" severity="success" [dismissible]="true" (dismissed)="message=''" />
      }

      <div class="bg-[var(--color-surface-raised)] border border-[var(--color-border)] rounded-xl p-5 mb-6">
        <h2 class="font-semibold mb-4 text-sm">Add CIDR Rule</h2>
        <form [formGroup]="form" (ngSubmit)="add()" class="flex flex-wrap gap-3 items-end">
          <div class="flex flex-col gap-1">
            <label class="text-xs font-medium">CIDR</label>
            <input formControlName="cidr" type="text" placeholder="192.168.1.0/24"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
          </div>
          <div class="flex flex-col gap-1">
            <label class="text-xs font-medium">Role scope</label>
            <select formControlName="roleScope"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px] bg-[var(--color-surface)]">
              <option value="">All roles</option>
              <option value="STUDENT">Student</option>
              <option value="CORPORATE_MENTOR">Corporate Mentor</option>
              <option value="FACULTY_MENTOR">Faculty Mentor</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>
          <div class="flex flex-col gap-1">
            <label class="text-xs font-medium">Note</label>
            <input formControlName="note" type="text" placeholder="Optional"
              class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
          </div>
          <app-button type="submit" variant="primary" [loading]="adding" [disabled]="form.invalid">Add</app-button>
        </form>
      </div>

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2]; track i) { <app-skeleton height="56px" /> }
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
          <table class="w-full text-sm">
            <thead class="bg-[var(--color-surface-raised)]">
              <tr>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">CIDR</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Role scope</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Note</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (r of ranges; track r.id) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3 font-mono text-xs">{{ r.cidr }}</td>
                  <td class="px-4 py-3 text-xs">{{ r.roleScope || 'All' }}</td>
                  <td class="px-4 py-3 text-xs text-[var(--color-text-muted)]">{{ r.note || '—' }}</td>
                  <td class="px-4 py-3 text-right">
                    <button (click)="remove(r.id)" class="text-xs text-[var(--color-danger)] hover:underline min-h-0">Delete</button>
                  </td>
                </tr>
              }
              @if (ranges.length === 0) {
                <tr><td colspan="4" class="px-4 py-8 text-center text-[var(--color-text-muted)]">No IP rules configured.</td></tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
})
export class AdminIpRangesComponent implements OnInit {
  ranges: IpRange[] = [];
  loading = false;
  adding = false;
  message = '';
  form: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.form = this.fb.group({
      cidr: ['', Validators.required],
      roleScope: [''],
      note: [''],
    });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<IpRange[]>('/api/v1/admin/allowed-ip-ranges').pipe(
      catchError(() => of([]))
    ).subscribe(r => { this.ranges = r; this.loading = false; });
  }

  add(): void {
    if (this.form.invalid || this.adding) return;
    this.adding = true;
    const { cidr, roleScope, note } = this.form.value;
    const body = {
      cidr,
      roleScope: roleScope || null,
      note: note || null,
    };
    this.http.post<IpRange>('/api/v1/admin/allowed-ip-ranges', body).pipe(
      catchError(() => of(null))
    ).subscribe(r => {
      this.adding = false;
      if (r) { this.ranges = [r, ...this.ranges]; this.form.reset(); this.message = 'Rule added.'; }
    });
  }

  remove(id: string): void {
    this.http.delete(`/api/v1/admin/allowed-ip-ranges/${id}`).pipe(catchError(() => of(null))).subscribe(() => {
      this.ranges = this.ranges.filter(r => r.id !== id);
    });
  }
}
