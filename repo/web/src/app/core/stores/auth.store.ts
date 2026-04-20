import { Injectable } from '@angular/core';
import { BehaviorSubject, map, Observable } from 'rxjs';
import { UserProfile, Role } from '../models/user.model';
import { clearTokens, storeTokens } from '../http/auth.interceptor';

@Injectable({ providedIn: 'root' })
export class AuthStore {
  private readonly _profile$ = new BehaviorSubject<UserProfile | null>(null);

  readonly profile$: Observable<UserProfile | null> = this._profile$.asObservable();
  readonly isAuthenticated$: Observable<boolean> = this._profile$.pipe(map(p => p !== null));

  isAuthenticated(): boolean {
    return this._profile$.getValue() !== null;
  }

  userRole(): Role | null {
    return this._profile$.getValue()?.role ?? null;
  }

  userStatus(): string | null {
    return this._profile$.getValue()?.status ?? null;
  }

  userId(): string | null {
    return this._profile$.getValue()?.id ?? null;
  }

  organizationId(): string | null {
    return this._profile$.getValue()?.organizationId ?? null;
  }

  setProfile(profile: UserProfile): void {
    this._profile$.next(profile);
    localStorage.setItem('meridian_profile', JSON.stringify(profile));
  }

  hydrateFromStorage(): void {
    const raw = localStorage.getItem('meridian_profile');
    if (raw) {
      try {
        this._profile$.next(JSON.parse(raw) as UserProfile);
      } catch {
        localStorage.removeItem('meridian_profile');
      }
    }
  }

  login(accessToken: string, refreshToken: string, profile: UserProfile): void {
    storeTokens(accessToken, refreshToken);
    this.setProfile(profile);
  }

  clearProfile(): void {
    this._profile$.next(null);
    localStorage.removeItem('meridian_profile');
    clearTokens();
  }
}
