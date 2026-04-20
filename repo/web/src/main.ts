import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { APP_INITIALIZER } from '@angular/core';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { db } from './app/core/db/dexie';
import { authInterceptor } from './app/core/http/auth.interceptor';
import { idempotencyInterceptor } from './app/core/http/idempotency.interceptor';
import { offlineInterceptor } from './app/core/http/offline.interceptor';
import { errorInterceptor } from './app/core/http/error.interceptor';
import { AuthStore } from './app/core/stores/auth.store';
import { BackgroundSyncService } from './app/core/http/background-sync.service';

db.open().catch(err => console.error('Dexie failed to open:', err));

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        offlineInterceptor,   // queue offline mutations first
        authInterceptor,      // add Bearer token + handle 401 refresh
        idempotencyInterceptor,
        errorInterceptor,
      ])
    ),
    {
      provide: APP_INITIALIZER,
      useFactory: (authStore: AuthStore, _sync: BackgroundSyncService) => () => {
        authStore.hydrateFromStorage();
      },
      deps: [AuthStore, BackgroundSyncService],
      multi: true,
    },
  ]
}).catch(err => console.error(err));
