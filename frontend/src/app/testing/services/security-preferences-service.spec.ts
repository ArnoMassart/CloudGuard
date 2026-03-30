import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PreferencesResponse } from '../../models/preferences/PreferencesResponse';
import { SecurityPreferencesService } from '../../services/security-preferences-service';

describe('SecurityPreferencesService', () => {
  let service: SecurityPreferencesService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SecurityPreferencesService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SecurityPreferencesService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAllPreferences', () => {
    it('GETs with credentials', () => {
      const body: PreferencesResponse = {
        preferences: { 'a:b': true },
        dnsImportance: { SPF: 'REQUIRED' },
        dnsImportanceOverrideTypes: ['SPF'],
      };
      service.getAllPreferences().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/user/preferences');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getDisabledKeys', () => {
    it('GETs disabled list with credentials', () => {
      const body = ['users-groups:2fa'];
      service.getDisabledKeys().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/user/preferences/disabled');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('setPreference', () => {
    it('PUTs boolean toggle with null value', () => {
      service.setPreference('users-groups', '2fa', false).subscribe();

      const req = httpMock.expectOne((r) => r.url === '/api/user/preferences');
      expect(req.request.method).toBe('PUT');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.body).toEqual({
        section: 'users-groups',
        preferenceKey: '2fa',
        enabled: false,
        value: null,
      });
      req.flush(null);
    });

    it('PUTs DNS importance with string value', () => {
      service.setPreference('domain-dns', 'impSpf', true, 'REQUIRED').subscribe();

      const req = httpMock.expectOne((r) => r.url === '/api/user/preferences');
      expect(req.request.body).toEqual({
        section: 'domain-dns',
        preferenceKey: 'impSpf',
        enabled: true,
        value: 'REQUIRED',
      });
      req.flush(null);
    });
  });
});
