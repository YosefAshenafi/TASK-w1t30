import { Injectable, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { filter, distinctUntilChanged } from 'rxjs/operators';
import { NetworkStatusService } from '../stores/network-status.service';
import { OutboxService } from './outbox.service';

@Injectable({ providedIn: 'root' })
export class BackgroundSyncService implements OnDestroy {
  private subscription?: Subscription;

  constructor(
    private network: NetworkStatusService,
    private outbox: OutboxService,
  ) {
    this.start();
  }

  private start(): void {
    // Drain outbox each time the app comes back online
    this.subscription = this.network.online$.pipe(
      distinctUntilChanged(),
      filter(online => online),
    ).subscribe(() => {
      this.outbox.drainPending().catch(console.error);
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}
