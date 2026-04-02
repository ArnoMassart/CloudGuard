import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DashboardService } from '../../services/dashboard-service';
import { DashboardPageResponse } from '../../models/dashboard/DashboardPageResponse';
import { DashboardOverviewResponse } from '../../models/dashboard/DashboardOverviewResponse';

describe('DashboardService', () => {
  let service: DashboardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DashboardService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DashboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDashboardData', () => {
    it('GETs dashboard data with credentials', () => {
      const body = {
        overallScore: 85,
      } as DashboardPageResponse;

      service.getDashboardData().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/dashboard'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getDashboardPageOverview', () => {
    it('GETs dashboard overview with credentials', () => {
      const body = {
        totalNotifications: 5,
        criticalNotifications: 1,
      } as DashboardOverviewResponse;

      service.getDashboardPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/dashboard/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });
});
