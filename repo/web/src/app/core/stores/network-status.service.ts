import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, interval, Subscription, catchError, of } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NetworkStatusService implements OnDestroy {
  private readonly _online$ = new BehaviorSubject<boolean>(true);
  readonly online$ = this._online$.asObservable();

  private pollingSubscription?: Subscription;
  private static readonly POLL_INTERVAL_MS = 15_000;

  constructor(private http: HttpClient) {
    this.startPolling();
  }

  isOnline(): boolean {
    return this._online$.getValue();
  }

  private startPolling(): void {
    this.pollingSubscription = interval(NetworkStatusService.POLL_INTERVAL_MS).subscribe(() => {
      this.http.get('/api/v1/health', { responseType: 'json' }).pipe(
        catchError(() => {
          this._online$.next(false);
          return of(null);
        })
      ).subscribe(result => {
        if (result !== null) {
          this._online$.next(true);
        }
      });
    });
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }
}
