import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: number;
  message: string;
  severity: 'success' | 'error' | 'warn' | 'info';
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private nextId = 0;
  private readonly _toasts$ = new BehaviorSubject<Toast[]>([]);
  readonly toasts$ = this._toasts$.asObservable();

  success(message: string, durationMs = 3000): void {
    this.show(message, 'success', durationMs);
  }

  error(message: string, durationMs = 5000): void {
    this.show(message, 'error', durationMs);
  }

  warn(message: string, durationMs = 4000): void {
    this.show(message, 'warn', durationMs);
  }

  info(message: string, durationMs = 3000): void {
    this.show(message, 'info', durationMs);
  }

  dismiss(id: number): void {
    this._toasts$.next(this._toasts$.getValue().filter(t => t.id !== id));
  }

  private show(message: string, severity: Toast['severity'], durationMs: number): void {
    const id = ++this.nextId;
    const current = this._toasts$.getValue();
    this._toasts$.next([...current, { id, message, severity }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }
}
