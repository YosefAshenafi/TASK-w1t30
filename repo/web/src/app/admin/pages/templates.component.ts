import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ButtonComponent } from '../../shared/ui/button.component';
import { SkeletonComponent } from '../../shared/ui/skeleton.component';
import { BannerComponent } from '../../shared/ui/banner.component';
import { DialogComponent } from '../../shared/ui/dialog.component';
import { catchError, of } from 'rxjs';

interface Template {
  key: string;
  titleTmpl: string;
  bodyTmpl: string;
  variables: string;
}

@Component({
  selector: 'app-admin-templates',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent, SkeletonComponent, BannerComponent, DialogComponent],
  template: `
    <div class="p-6 max-w-5xl mx-auto">
      <h1 class="text-xl font-bold mb-4">Notification Templates</h1>

      @if (message) {
        <app-banner [message]="message" severity="success" [dismissible]="true" (dismissed)="message=''" />
      }

      @if (loading) {
        <div class="flex flex-col gap-3">
          @for (i of [1,2,3]; track i) { <app-skeleton height="56px" /> }
        </div>
      } @else {
        <div class="overflow-x-auto rounded-lg border border-[var(--color-border)]">
          <table class="w-full text-sm">
            <thead class="bg-[var(--color-surface-raised)]">
              <tr>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Key</th>
                <th class="px-4 py-3 text-left font-medium text-[var(--color-text-muted)]">Title template</th>
                <th class="px-4 py-3 text-right font-medium text-[var(--color-text-muted)]">Actions</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[var(--color-border)]">
              @for (t of templates; track t.key) {
                <tr class="hover:bg-[var(--color-surface-raised)]">
                  <td class="px-4 py-3 font-mono text-xs">{{ t.key }}</td>
                  <td class="px-4 py-3 text-sm">{{ t.titleTmpl }}</td>
                  <td class="px-4 py-3 text-right">
                    <button (click)="openEdit(t)" class="text-xs text-[var(--color-brand-600)] hover:underline min-h-0">Edit</button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>

    <app-dialog [open]="editOpen" [title]="'Edit: ' + editKey" [hasFooter]="true" (closed)="editOpen=false">
      <form [formGroup]="editForm" (ngSubmit)="saveEdit()" class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Title template</label>
          <input formControlName="titleTmpl" type="text"
            class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none min-h-[48px]" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-sm font-medium">Body template (Markdown)</label>
          <textarea formControlName="bodyTmpl" rows="6"
            class="border border-[var(--color-border)] rounded-lg px-3 py-2 text-sm focus:outline-none resize-y font-mono"></textarea>
        </div>
      </form>
      <div slot="footer">
        <app-button variant="ghost" size="sm" (click)="editOpen=false">Cancel</app-button>
        <app-button variant="primary" size="sm" [loading]="saving" (click)="saveEdit()">Save</app-button>
      </div>
    </app-dialog>
  `,
})
export class AdminTemplatesComponent implements OnInit {
  templates: Template[] = [];
  loading = false;
  message = '';
  editOpen = false;
  editKey = '';
  saving = false;
  editForm: FormGroup;

  constructor(private fb: FormBuilder, private http: HttpClient) {
    this.editForm = this.fb.group({
      titleTmpl: ['', Validators.required],
      bodyTmpl: ['', Validators.required],
    });
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.http.get<Template[]>('/api/v1/admin/notification-templates').pipe(
      catchError(() => of([]))
    ).subscribe(t => { this.templates = t; this.loading = false; });
  }

  openEdit(t: Template): void {
    this.editKey = t.key;
    this.editForm.setValue({ titleTmpl: t.titleTmpl, bodyTmpl: t.bodyTmpl });
    this.editOpen = true;
  }

  saveEdit(): void {
    if (this.editForm.invalid || this.saving) return;
    this.saving = true;
    this.http.put(`/api/v1/admin/notification-templates/${this.editKey}`, this.editForm.value).pipe(
      catchError(() => of(null))
    ).subscribe(ok => {
      this.saving = false;
      if (ok !== null) { this.editOpen = false; this.message = 'Template updated.'; this.load(); }
    });
  }
}
