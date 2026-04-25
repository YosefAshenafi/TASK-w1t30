import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { authGuard } from './auth.guard';
import { AuthStore } from '../stores/auth.store';

describe('authGuard', () => {
  let authStore: AuthStore;
  let router: Router;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = { url: '/home' } as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
    });
    authStore = TestBed.inject(AuthStore);
    router = TestBed.inject(Router);
  });

  it('returns true when authenticated', () => {
    spyOn(authStore, 'isAuthenticated').and.returnValue(true);
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).toBeTrue();
  });

  it('redirects to login when not authenticated', () => {
    spyOn(authStore, 'isAuthenticated').and.returnValue(false);
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
    expect(result).not.toBeTrue();
    // Result should be a UrlTree pointing to /login
    const urlTree = result as ReturnType<Router['createUrlTree']>;
    expect(urlTree.toString()).toContain('login');
  });

  it('passes returnUrl as query parameter', () => {
    spyOn(authStore, 'isAuthenticated').and.returnValue(false);
    const stateWithUrl = { url: '/admin/users' } as RouterStateSnapshot;
    const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, stateWithUrl));
    const urlTree = result as ReturnType<Router['createUrlTree']>;
    expect(urlTree.toString()).toContain('returnUrl');
  });
});
