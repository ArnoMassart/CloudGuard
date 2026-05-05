import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserPageResponse } from '../../models/users/UserPageResponse';
import { UserService } from '../../services/user-service';
import { Role, RoleLabels } from '../../models/users/User';

describe('UserService Integration', () => {
  let service: UserService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [UserService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(UserService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Controleer of er geen onverwachte HTTP-verzoeken openstaan
    httpTesting.verify();
  });

  describe('Google Workspace Users (GET)', () => {
    it('should build correct HttpParams and include withCredentials for getOrgUsers', () => {
      const mockResponse: UserPageResponse = { users: [], nextPageToken: 'abc' } as any;

      service.getOrgUsers(10, 'token123', 'search-term').subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('pageToken')).toBe('token123');
      expect(req.request.params.get('query')).toBe('search-term');

      req.flush(mockResponse);
    });

    it('should fetch users overview', () => {
      service.getUsersPageOverview().subscribe();
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush({});
    });

    it('should fetch users without 2FA', () => {
      service.getUsersWithoutTwoFactor().subscribe();
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users/without-two-factor'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush({});
    });
  });

  describe('Cache & Utilities (POST)', () => {
    it('should send a POST request to refresh cache and handle text response', () => {
      const successMessage = 'Cache refreshed';

      service.refreshUsersCache().subscribe((res) => {
        expect(res).toBe(successMessage);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users/refresh'));

      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      expect(req.request.responseType).toBe('text');

      req.flush(successMessage);
    });
  });

  describe('User Preferences & Settings', () => {
    it('should update language via POST', () => {
      service.updateLanguage('nl').subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/language'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ language: 'nl' });
      req.flush({});
    });

    it('should fetch language as a plain string', () => {
      const mockLang = 'en';
      service.getLanguage().subscribe((lang) => expect(lang).toBe(mockLang));

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/language'));
      expect(req.request.responseType).toBe('text');
      req.flush(mockLang);
    });

    it('should send role access request', () => {
      service.requestAccess('/request-access').subscribe();
      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/request-access'));
      expect(req.request.method).toBe('POST');
      req.flush('success');
    });

    it('should check if role access is sent', () => {
      service.getRequestSent('/request-access').subscribe((res: boolean) => expect(res).toBeTruthy());
      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/request-access'));
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });

    it('should check for valid roles', () => {
      service.hasValidField('/valid-role').subscribe((res: boolean) => expect(res).toBeTruthy());
      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/valid-role'));
      expect(req.request.method).toBe('GET');
      req.flush(true);
    });
  });

  describe('Database Users Management', () => {
    it('should fetch all database users with params', () => {
      service.getAllDatabaseUsers(5, 'page2', 'john').subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/all'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('pageToken')).toBe('page2');
      expect(req.request.params.get('query')).toBe('john');
      req.flush({ users: [], nextPageToken: '' });
    });

    it('should fetch requested-access database users', () => {
      service.getAllRequestedDatabaseUsers(5).subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/all/requested-access'));
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.has('pageToken')).toBeFalsy();
      req.flush({ users: [], nextPageToken: '' });
    });

    it('should update roles for user', () => {
      const roles = [Role.SUPER_ADMIN];
      service.updateRolesForUser('test@domain.com', roles).subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/roles'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ userEmail: 'test@domain.com', roles });
      expect(req.request.withCredentials).toBe(true);
      req.flush({});
    });

    it('should update roles for user without triggering requested-count refresh', () => {
      const roles = [Role.USERS_GROUPS_VIEWER];

      service.updateRolesForUserWithoutAny('test@domain.com', roles).subscribe();

      const postReq = httpTesting.expectOne((r) => r.url.endsWith('/user/roles-without'));
      expect(postReq.request.method).toBe('POST');
      postReq.flush({});
    });

    it('should refresh requested count signal directly', () => {
      // Beginwaarde is 0
      expect(service.requestedCount()).toBe(0);

      service.refreshRequestedCount().subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/user/access-requests/count'));
      expect(req.request.method).toBe('GET');
      req.flush(8);

      // Waarde in de signal moet nu 8 zijn
      expect(service.requestedCount()).toBe(8);
    });
  });

  describe('Pure Logic (Initials & Roles)', () => {
    it('should generate initials correctly', () => {
      expect(service.getInitials({ firstName: 'John', lastName: 'Doe' })).toBe('JD');
      expect(service.getInitials({ firstName: 'Alice' })).toBe('AL');
      expect(service.getInitials({ email: 'bob@example.com' })).toBe('BO');
      expect(service.getInitials({})).toBe('?');
    });

    it('should return the fallback User label if no roles are present', () => {
      expect(service.getRole([])).toBe('User');
    });

    it('should sort roles by priority and return the label of the highest priority role', () => {
      // Gebruik Enum waarden die in RolePriority gedefinieerd staan.
      // Super Admin heeft de hoogste prioriteit (laagste getal)
      const roles = [Role.USERS_GROUPS_VIEWER, Role.SUPER_ADMIN, Role.DEVICES_VIEWER];

      const expectedLabel = RoleLabels[Role.SUPER_ADMIN];

      expect(service.getRole(roles)).toBe(expectedLabel);
    });

    it('should map a single role directly to its label', () => {
      const label = RoleLabels[Role.USERS_GROUPS_VIEWER];
      expect(service.getRoleLabel(Role.USERS_GROUPS_VIEWER)).toBe(label);
    });

    it('should return the raw string if the role has no mapped label', () => {
      expect(service.getRoleLabel('UNKNOWN_NEW_ROLE')).toBe('UNKNOWN_NEW_ROLE');
    });
  });
});
