import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { UserService } from '../../services/user-service';
import { UserPageResponse } from '../../models/users/UserPageResponse';
import { UserOverviewResponse } from '../../models/users/UserOverviewResponse';
import { UsersWithoutTwoFactorResponse } from '../../models/users/UsersWithoutTwoFactorResponse';

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

  describe('getRole', () => {
    it('returns the first role from the array', () => {
      const role = service.getRole({ roles: ['Super Admin', 'User'] });
      expect(role).toBe('Super Admin');
    });

    it('returns "Admin" if roles array is empty', () => {
      const role = service.getRole({ roles: [] });
      expect(role).toBe('Admin');
    });

    it('returns empty string if roles array is undefined', () => {
      const role = service.getRole({ roles: undefined as unknown as string[] });
      expect(role).toBe('');
    });
  });

  describe('getOrgUsers', () => {
    it('sends size and omits query/pageToken when not provided', () => {
      // nextPageToken is een string volgens je model, dus we gebruiken een lege string i.p.v. null
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
      // Bijgewerkt met de velden uit jouw UserOverviewResponse interface
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

      expect(req.request.body).toEqual({ withCredentials: true });
      req.flush('OK');
    });
  });

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
});
