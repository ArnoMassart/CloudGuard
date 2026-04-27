import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { roleGuard } from './role.guard';
import { CustomAuthService } from '../auth/custom-auth-service';
import { Role, User } from '../models/users/User';

function minimalUser(roles: Role[]): User {
  return {
    email: 'user@example.com',
    firstName: 'U',
    lastName: 'User',
    roles,
    createdAt: new Date(),
    roleRequested: false,
    organizationRequested: false,
    organizationId: 1,
  };
}

function runRoleGuard(routeData: Record<string, unknown>) {
  return TestBed.runInInjectionContext(() =>
    roleGuard(
      { data: routeData } as unknown as ActivatedRouteSnapshot,
      {} as RouterStateSnapshot,
    ),
  );
}

describe('roleGuard', () => {
  let router: Router;

  function setup(auth: { currentUser: ReturnType<typeof signal<User | null>>; isInitialized$: BehaviorSubject<boolean> }) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: CustomAuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
  }

  it('returns true when requiredRoles is missing or empty (no role check)', () => {
    setup({
      currentUser: signal<User | null>(null),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({});
    expect(result).toBe(true);
  });

  it('allows users with SUPER_ADMIN for any requiredRoles', async () => {
    setup({
      currentUser: signal<User | null>(minimalUser([Role.SUPER_ADMIN])),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({ requiredRoles: [Role.DEVICES_VIEWER] });
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);
    expect(resolved).toBe(true);
  });

  it('allows access when the user has one of the required roles', async () => {
    setup({
      currentUser: signal<User | null>(minimalUser([Role.DEVICES_VIEWER])),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({ requiredRoles: [Role.DEVICES_VIEWER, Role.APP_ACCESS_VIEWER] });
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);
    expect(resolved).toBe(true);
  });

  it('redirects to /no-page-access when user lacks the required role', async () => {
    setup({
      currentUser: signal<User | null>(minimalUser([Role.LICENSES_VIEWER])),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({ requiredRoles: [Role.DEVICES_VIEWER] });
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);
    expect(resolved instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(resolved as UrlTree)).toBe('/no-page-access');
  });

  it('redirects to /no-access when there is no current user', async () => {
    setup({
      currentUser: signal<User | null>(null),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({ requiredRoles: [Role.DEVICES_VIEWER] });
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);
    expect(resolved instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(resolved as UrlTree)).toBe('/no-access');
  });

  it('redirects to /no-access when roles array is empty', async () => {
    setup({
      currentUser: signal<User | null>(minimalUser([])),
      isInitialized$: new BehaviorSubject(true),
    });
    const result = runRoleGuard({ requiredRoles: [Role.DEVICES_VIEWER] });
    const resolved = await firstValueFrom(result as Observable<boolean | UrlTree>);
    expect(resolved instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(resolved as UrlTree)).toBe('/no-access');
  });
});
