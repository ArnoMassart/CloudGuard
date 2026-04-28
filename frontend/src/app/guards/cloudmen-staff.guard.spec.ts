import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideRouter, Router, UrlTree, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';
import { cloudmenStaffGuard } from './cloudmen-staff.guard';
import { CustomAuthService } from '../auth/custom-auth-service';

function runCloudmenStaffGuard() {
  return TestBed.runInInjectionContext(() =>
    cloudmenStaffGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  );
}

describe('cloudmenStaffGuard', () => {
  let router: Router;

  function setup(auth: {
    isCloudmenStaff: ReturnType<typeof signal<boolean>>;
    isInitialized$: BehaviorSubject<boolean>;
  }) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: CustomAuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
  }

  it('waits for initialization, then allows staff users', async () => {
    const isInitialized$ = new BehaviorSubject(false);
    setup({
      isCloudmenStaff: signal(true),
      isInitialized$,
    });
    const result$ = runCloudmenStaffGuard() as Observable<boolean | UrlTree>;
    const p = firstValueFrom(result$);
    isInitialized$.next(true);
    const resolved = await p;
    expect(resolved).toBe(true);
  });

  it('returns UrlTree to /no-page-access when user is not Cloudmen staff', async () => {
    setup({
      isCloudmenStaff: signal(false),
      isInitialized$: new BehaviorSubject(true),
    });
    const result$ = runCloudmenStaffGuard() as Observable<boolean | UrlTree>;
    const resolved = await firstValueFrom(result$);
    expect(resolved instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(resolved as UrlTree)).toBe('/no-page-access');
  });
});
