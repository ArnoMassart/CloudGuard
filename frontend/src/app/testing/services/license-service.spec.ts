import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { LicenseService } from '../../services/license-service';
import { LicensePageResponse } from '../../models/licenses/LicensePageResponse';
import { LicenseOverviewResponse } from '../../models/licenses/LicenseOverviewResponse';

describe('LicenseService', () => {
  let service: LicenseService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LicenseService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(LicenseService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getLicenses', () => {
    it('GETs licenses with credentials', () => {
      const body = {
        licenses: [],
      } as unknown as LicensePageResponse;

      service.getLicenses().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/license'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getLicensesPageOverview', () => {
    it('GETs license overview with credentials', () => {
      const body = {
        totalLicenses: 100,
        assignedLicenses: 80,
        unassignedLicenses: 20,
        securityScore: 100,
      } as unknown as LicenseOverviewResponse;

      service.getLicensesPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/license/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });
});
