import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OAuthService } from '../../services/o-auth-service';
import { OAuthPagedResponse } from '../../models/o-auth/OAuthPagedResponse';
import { OAuthOverviewResponse } from '../../models/o-auth/OAuthOverviewResponse';
import { Risk } from '../../models/o-auth/Risk';

describe('OAuthService', () => {
  let service: OAuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OAuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OAuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getApps', () => {
    it('sends size and risk, and omits query/pageToken when not provided', () => {
      const body = { apps: [], nextPageToken: '' } as unknown as OAuthPagedResponse;
      const testRisk = 'all' as Risk;

      service.getApps(10, testRisk).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) =>
          r.url.endsWith('/google/oAuth') &&
          r.params.get('size') === '10' &&
          r.params.get('risk') === testRisk
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('query')).toBe(false);
      expect(req.request.params.has('pageToken')).toBe(false);
      req.flush(body);
    });

    it('sends all params including pageToken and query when provided', () => {
      const body = { apps: [], nextPageToken: 'next123' } as unknown as OAuthPagedResponse;
      const testRisk = 'high' as Risk;

      service
        .getApps(5, testRisk, 'page1', 'marketing')
        .subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/oAuth'));
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('risk')).toBe(testRisk);
      expect(req.request.params.get('pageToken')).toBe('page1');
      expect(req.request.params.get('query')).toBe('marketing');
      req.flush(body);
    });
  });

  describe('getOAuthPageOverview', () => {
    it('GETs overview with credentials', () => {
      const body = {
        totalThirdPartyApps: 25,
        totalHighRiskApps: 3,
        securityScore: 80,
      } as unknown as OAuthOverviewResponse;

      service.getOAuthPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/oAuth/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshOAuthCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshOAuthCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/oAuth/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({ withCredentials: true });
      req.flush('OK');
    });
  });
});
