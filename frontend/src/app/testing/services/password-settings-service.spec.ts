import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PasswordSettings } from '../../models/password/PasswordSettings';
import { PasswordSettingsService } from '../../services/password-settings-service';

const minimalSettings = (): PasswordSettings => ({
  passwordPoliciesByOu: [],
  twoStepVerification: {
    byOrgUnit: [],
    totalEnrolled: 0,
    totalEnforced: 0,
    totalUsers: 0,
  },
  usersWithForcedChange: [],
  summary: {
    usersWithForcedChange: 0,
    usersWith2SvEnrolled: 0,
    usersWith2SvEnforced: 0,
    totalUsers: 0,
  },
  adminsWithoutSecurityKeys: [],
  securityScore: 100,
});

describe('PasswordSettingsService', () => {
  let service: PasswordSettingsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PasswordSettingsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(PasswordSettingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getPasswordSettings', () => {
    it('GETs settings with credentials', () => {
      const body = minimalSettings();
      service.getPasswordSettings().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/password-settings');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshCache', () => {
    it('POSTs refresh with credentials and text response', () => {
      service.refreshCache().subscribe((text) => expect(text).toBe('ok'));

      const req = httpMock.expectOne((r) => r.url === '/api/google/password-settings/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({});
      req.flush('ok');
    });
  });
});
