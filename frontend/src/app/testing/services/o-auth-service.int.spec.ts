import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OAuthOverviewResponse } from '../../models/o-auth/OAuthOverviewResponse';
import { OAuthPagedResponse } from '../../models/o-auth/OAuthPagedResponse';
import { OAuthService } from '../../services/o-auth-service';

describe('OAuthService Integration', () => {
  let service: OAuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OAuthService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(OAuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Zorgt ervoor dat er geen onafgemaakte HTTP-requests rondslingeren
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getApps (Filtering & Pagination)', () => {
    it('should correctly set all query parameters including risk', () => {
      const mockResponse: OAuthPagedResponse = {
        apps: [],
        nextPageToken: 'token-123',
        allFilteredApps: 0,
        allHighRiskApps: 0,
        allNotHighRiskApps: 0,
      } as unknown as OAuthPagedResponse;

      service.getApps(10, 'high', 'page-1', 'search-query').subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      // Controleer de parameters
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('risk')).toBe('high');
      expect(req.request.params.get('pageToken')).toBe('page-1');
      expect(req.request.params.get('query')).toBe('search-query');

      req.flush(mockResponse);
    });

    it('should only include size and risk when optional params are omitted', () => {
      service.getApps(5, 'all').subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth'));
      expect(req.request.params.has('size')).toBe(true);
      expect(req.request.params.has('risk')).toBe(true);
      expect(req.request.params.has('pageToken')).toBe(false);
      expect(req.request.params.has('query')).toBe(false);

      req.flush({});
    });
  });

  describe('getOAuthPageOverview', () => {
    it('should fetch overview data from the correct sub-path', () => {
      const mockOverview: OAuthOverviewResponse = {
        totalThirdPartyApps: 50,
        totalHighRiskApps: 5,
        securityScore: 82,
      } as unknown as OAuthOverviewResponse;

      service.getOAuthPageOverview().subscribe((data) => {
        expect(data).toEqual(mockOverview);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockOverview);
    });
  });

  describe('refreshOAuthCache', () => {
    it('should POST to the refresh endpoint and expect text back', () => {
      const mockMsg = 'OAuth cache refreshed';

      service.refreshOAuthCache().subscribe((res) => {
        expect(res).toBe(mockMsg);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/refresh'));

      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      // Verifieer dat de data in de body zit zoals gedefinieerd in je service
      expect(req.request.body).toEqual({ withCredentials: true });

      req.flush(mockMsg);
    });
  });

  describe('Error Handling', () => {
    it('should pass through 401 Unauthorized errors', () => {
      service.getOAuthPageOverview().subscribe({
        next: () => expect.fail('Request had moeten falen'),
        error: (error) => {
          expect(error.status).toBe(401);
        },
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/oAuth/overview'));
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });
  });
});
