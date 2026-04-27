import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '@auth0/auth0-angular';
import { CustomAuthService } from './custom-auth-service';
import { User, Role } from '../models/users/User';
import { UserService } from '../services/user-service';
import { WarmupCacheService } from '../services/warmup-cache-service';

describe('CustomAuthService', () => {
  let service: CustomAuthService;
  let httpMock: HttpTestingController;
  let isCloudmenStaffMock: ReturnType<typeof vi.fn>;

  const minimalUser: User = {
    email: 'u@example.com',
    firstName: 'U',
    lastName: 'User',
    roles: [Role.DEVICES_VIEWER],
    createdAt: new Date(),
    roleRequested: false,
    organizationRequested: false,
    organizationId: 1,
  };

  beforeEach(() => {
    isCloudmenStaffMock = vi.fn(() => of(true));
    TestBed.configureTestingModule({
      providers: [
        CustomAuthService,
        { provide: UserService, useValue: { isCloudmenStaff: isCloudmenStaffMock } },
        { provide: AuthService, useValue: { logout: vi.fn() } },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: WarmupCacheService, useValue: {} },
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(CustomAuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('loginWithGoogle', () => {
    it('POSTs the id token to /api/auth/login', () => {
      service.loginWithGoogle('google-id-token').subscribe();
      const loginReq = httpMock.expectOne('/api/auth/login');
      expect(loginReq.request.method).toBe('POST');
      expect(loginReq.request.body).toEqual({ token: 'google-id-token' });
      loginReq.flush(minimalUser);
      httpMock.expectOne('/api/auth/me').flush(minimalUser);
    });

    it('refreshes Cloudmen staff flag after a successful login', () => {
      service.loginWithGoogle('google-id-token').subscribe();
      httpMock.expectOne('/api/auth/login').flush(minimalUser);
      httpMock.expectOne('/api/auth/me').flush(minimalUser);
      expect(isCloudmenStaffMock).toHaveBeenCalled();
    });
  });
});
