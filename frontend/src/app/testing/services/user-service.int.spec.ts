import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserPageResponse } from '../../models/users/UserPageResponse';
import { UserService } from '../../services/user-service';

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

  describe('getOrgUsers (HTTP Params & Credentials)', () => {
    it('should build correct HttpParams and include withCredentials', () => {
      const mockResponse: UserPageResponse = { users: [], nextPageToken: 'abc' } as any;

      // 1. Roep de methode aan
      service.getOrgUsers(10, 'token123', 'search-term').subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      // 2. Verwacht een GET verzoek met specifieke query parameters
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('pageToken')).toBe('token123');
      expect(req.request.params.get('query')).toBe('search-term');

      // 3. Geef data terug aan de stream
      req.flush(mockResponse);
    });
  });

  describe('refreshUsersCache (POST with Text Response)', () => {
    it('should send a POST request and handle text response', () => {
      const successMessage = 'Cache refreshed';

      service.refreshUsersCache().subscribe((res) => {
        expect(res).toBe(successMessage);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/users/refresh'));

      expect(req.request.method).toBe('POST');
      // Let op: in je code stuur je { withCredentials: true } mee in de BODY van de POST
      expect(req.request.body).toEqual({});
      expect(req.request.responseType).toBe('text');

      req.flush(successMessage);
    });
  });

  describe('Language Management', () => {
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
  });

  describe('Pure Logic (Initials & Roles)', () => {
    // Hoewel dit unit-test-waardig is, testen we het hier mee
    // om te garanderen dat de service-instantie correct werkt.

    it('should generate initials correctly', () => {
      expect(service.getInitials({ firstName: 'John', lastName: 'Doe' })).toBe('JD');
      expect(service.getInitials({ firstName: 'Alice' })).toBe('AL');
      expect(service.getInitials({ email: 'bob@example.com' })).toBe('BO');
      expect(service.getInitials({})).toBe('?');
    });

    it('should return the first role or Admin default', () => {
      expect(service.getRole({ roles: ['Security Admin', 'User'] })).toBe('Security Admin');
      expect(service.getRole({ roles: [] })).toBe('Admin');
    });
  });
});
