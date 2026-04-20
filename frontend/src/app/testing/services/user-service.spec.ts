import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserService } from '../../services/user-service';
import { UserPageResponse } from '../../models/users/UserPageResponse';
import { UserOverviewResponse } from '../../models/users/UserOverviewResponse';
import { UsersWithoutTwoFactorResponse } from '../../models/users/UsersWithoutTwoFactorResponse';
import { DatabaseUsersResponse } from '../../models/users/DatabaseUsersResponse';
import { Role, RoleLabels } from '../../models/users/User';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ==========================================
  // PURE LOGIC TESTS (Initials, Roles)
  // ==========================================

  describe('getInitials', () => {
    it('returns first letters of firstName and lastName', () => {
      const initials = service.getInitials({ firstName: 'John', lastName: 'Doe' });
      expect(initials).toBe('JD');
    });

    it('returns first two letters of firstName if no lastName', () => {
      const initials = service.getInitials({ firstName: 'Alice' });
      expect(initials).toBe('AL');
    });

    it('returns first two letters of email if no names provided', () => {
      const initials = service.getInitials({ email: 'bob@example.com' });
      expect(initials).toBe('BO');
    });

    it('returns "?" if no valid properties exist', () => {
      const initials = service.getInitials({});
      expect(initials).toBe('?');
    });
  });

  describe('getRole & getRoleLabel', () => {
    it('returns the label of the highest priority role', () => {
      // Zelfs als een lagere-prioriteit rol als eerste in de array staat,
      // moet SUPER_ADMIN winnen dankzij de RolePriority
      const roles = [Role.USERS_GROUPS_VIEWER, Role.SUPER_ADMIN];
      const roleLabel = service.getRole(roles);
      expect(roleLabel).toBe(RoleLabels[Role.SUPER_ADMIN]);
    });

    it('returns "User" if roles array is empty', () => {
      const role = service.getRole([]);
      expect(role).toBe('User');
    });

    it('returns "User" if roles array is undefined or null', () => {
      const role = service.getRole(undefined as unknown as Role[]);
      expect(role).toBe('User');
    });

    it('getRoleLabel maps a single role to its translation key', () => {
      expect(service.getRoleLabel(Role.DEVICES_VIEWER)).toBe(RoleLabels[Role.DEVICES_VIEWER]);
    });

    it('getRoleLabel returns the raw string if no translation key exists', () => {
      expect(service.getRoleLabel('UNKNOWN_NEW_ROLE' as Role)).toBe('UNKNOWN_NEW_ROLE');
    });
  });

  // ==========================================
  // HTTP TESTS - GOOGLE WORKSPACE API
  // ==========================================

  describe('getOrgUsers', () => {
    it('sends size and omits query/pageToken when not provided', () => {
      const body: UserPageResponse = { users: [], nextPageToken: '' };
      service.getOrgUsers(10).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) => r.url.endsWith('/google/users') && r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('query')).toBe(false);
      expect(req.request.params.has('pageToken')).toBe(false);
      req.flush(body);
    });

    it('sends pageToken and query when provided', () => {
      const body: UserPageResponse = { users: [], nextPageToken: 'next123' };
      service.getOrgUsers(5, 'page1', 'admin').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/users'));
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('pageToken')).toBe('page1');
      expect(req.request.params.get('query')).toBe('admin');
      req.flush(body);
    });
  });

  describe('getUsersPageOverview', () => {
    it('GETs overview with credentials', () => {
      const body: UserOverviewResponse = {
        totalUsers: 100,
        adminUsers: 5,
        withoutTwoFactor: 2,
        activeLongNoLoginCount: 1,
        inactiveRecentLoginCount: 0,
        securityScore: 95,
      };
      service.getUsersPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/users/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getUsersWithoutTwoFactor', () => {
    it('GETs users without 2FA with credentials', () => {
      const body: UsersWithoutTwoFactorResponse = { users: [] };
      service.getUsersWithoutTwoFactor().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/users/without-two-factor'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshUsersCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshUsersCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/users/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({});
      req.flush('OK');
    });
  });

  // ==========================================
  // HTTP TESTS - USER PREFERENCES & ACCESS
  // ==========================================

  describe('Language settings', () => {
    it('updateLanguage POSTs language selection', () => {
      service.updateLanguage('nl').subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/language'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ language: 'nl' });
      req.flush(null);
    });

    it('getLanguage GETs language preference with credentials', () => {
      service.getLanguage().subscribe((res) => expect(res).toBe('nl'));

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/language'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.responseType).toBe('text');
      req.flush('nl');
    });
  });

  describe('Role Access Requests', () => {
    it('requestRoleAccess POSTs successfully', () => {
      service.requestRoleAccess().subscribe();
      const req = httpMock.expectOne((r) => r.url.endsWith('/user/request-access'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      req.flush('OK');
    });

    it('getRequestRoleAccessSent GETs boolean status', () => {
      service.getRequestRoleAccessSent().subscribe((res) => expect(res).toBeTruthy());
      const req = httpMock.expectOne((r) => r.url.endsWith('/user/request-access'));
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });

    it('hasValidRole GETs boolean status', () => {
      service.hasValidRole().subscribe((res) => expect(res).toBeTruthy());
      const req = httpMock.expectOne((r) => r.url.endsWith('/user/valid-role'));
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  // ==========================================
  // HTTP TESTS - DATABASE USERS & ROLES
  // ==========================================

  describe('Database Users & Roles Management', () => {
    it('getAllDatabaseUsers fetches users with parameters', () => {
      const mockResponse: DatabaseUsersResponse = { users: [], nextPageToken: 'token' };
      service
        .getAllDatabaseUsers(15, 'token1', 'query')
        .subscribe((res) => expect(res).toEqual(mockResponse));

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/all'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('15');
      expect(req.request.params.get('pageToken')).toBe('token1');
      expect(req.request.params.get('query')).toBe('query');
      req.flush(mockResponse);
    });

    it('getAllDatabaseUsersWithoutRoles fetches users without roles', () => {
      const mockResponse: DatabaseUsersResponse = { users: [], nextPageToken: '' };
      service
        .getAllDatabaseUsersWithoutRoles(10)
        .subscribe((res) => expect(res).toEqual(mockResponse));

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/all/no-roles'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.has('pageToken')).toBeFalsy();
      req.flush(mockResponse);
    });

    it('updateRolesForUser POSTs roles for standard user', () => {
      const roles = [Role.DEVICES_VIEWER];
      service.updateRolesForUser('test@test.com', roles).subscribe();

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/roles'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ userEmail: 'test@test.com', roles });
      req.flush({});
    });

    it('refreshRequestedCount GETs count and updates the signal', () => {
      expect(service.requestedCount()).toBe(0); // Initiele state

      service.refreshRequestedCount();

      const req = httpMock.expectOne((r) => r.url.endsWith('/user/all/requested-count'));
      expect(req.request.method).toBe('GET');
      req.flush(42);

      expect(service.requestedCount()).toBe(42); // Signal moet nu geüpdatet zijn
    });

    it('updateRolesForUserWithoutAny updates roles AND triggers count refresh via tap', () => {
      const roles = [Role.SUPER_ADMIN];

      // Act
      service.updateRolesForUserWithoutAny('test@test.com', roles).subscribe();

      // Assert 1: De POST request moet gestuurd worden
      const postReq = httpMock.expectOne((r) => r.url.endsWith('/user/roles-without'));
      expect(postReq.request.method).toBe('POST');
      expect(postReq.request.body).toEqual({ userEmail: 'test@test.com', roles });
      postReq.flush({});

      // Assert 2: Vanwege de .pipe(tap(...)) in de service, moet er direct een GET volgen
      const countReq = httpMock.expectOne((r) => r.url.endsWith('/user/all/requested-count'));
      expect(countReq.request.method).toBe('GET');
      countReq.flush(5);

      // Assert 3: De signal moet up-to-date zijn met de nieuwe data
      expect(service.requestedCount()).toBe(5);
    });
  });
});
