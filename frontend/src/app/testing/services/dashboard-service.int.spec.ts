import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardOverviewResponse } from '../../models/dashboard/DashboardOverviewResponse';
import { DashboardPageResponse } from '../../models/dashboard/DashboardPageResponse';
import { DashboardService } from '../../services/dashboard-service';

describe('DashboardService Integration', () => {
  let service: DashboardService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DashboardService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Garandeert dat er geen onverwachte requests zijn blijven hangen
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getDashboardData', () => {
    it('should perform a GET request to the base dashboard URL', () => {
      const mockResponse = { overallScore: 85 } as DashboardPageResponse;

      service.getDashboardData().subscribe((data) => {
        expect(data).toEqual(mockResponse);
        expect(data.overallScore).toBe(85);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/dashboard'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockResponse);
    });
  });

  describe('getDashboardPageOverview', () => {
    it('should perform a GET request to the /overview sub-path', () => {
      const mockOverview = {
        totalCriticals: 5,
        securityScore: 92,
      } as unknown as DashboardOverviewResponse;

      service.getDashboardPageOverview().subscribe((data) => {
        expect(data).toEqual(mockOverview);
      });

      // We testen hier specifiek of de integratie met RouteService de URL correct opbouwt
      const req = httpTesting.expectOne((r) => r.url.endsWith('/dashboard/overview'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockOverview);
    });
  });

  describe('Error Handling Integration', () => {
    it('should propagate HTTP errors to the subscriber', () => {
      service.getDashboardData().subscribe({
        next: () => expect.fail('Zou een error moeten geven'),
        error: (error) => {
          expect(error.status).toBe(500);
        },
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/dashboard'));
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
