import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LicenseOverviewResponse } from '../../models/licenses/LicenseOverviewResponse';
import { LicensePageResponse } from '../../models/licenses/LicensePageResponse';
import { LicenseService } from '../../services/license-service';

describe('LicenseService Integration', () => {
  let service: LicenseService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [LicenseService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(LicenseService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verifieert dat er geen onverwachte requests zijn blijven hangen
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getLicenses', () => {
    it('should fetch license data via GET with credentials', () => {
      const mockResponse: LicensePageResponse = {
        licenses: [],
        totalCount: 0,
      } as unknown as LicensePageResponse;

      service.getLicenses().subscribe((data) => {
        expect(data).toEqual(mockResponse);
      });

      // Verwacht een request naar de basis license URL
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/license'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockResponse);
    });
  });

  describe('getLicensesPageOverview', () => {
    it('should fetch overview data from the sub-path /overview', () => {
      const mockOverview: LicenseOverviewResponse = {
        totalLicenses: 100,
        unassignedLicenses: 10,
        securityScore: 95,
      } as unknown as LicenseOverviewResponse;

      service.getLicensesPageOverview().subscribe((data) => {
        expect(data).toEqual(mockOverview);
      });

      // Controleer of de URL correct is opgebouwd door RouteService + de toevoeging
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/license/overview'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockOverview);
    });
  });

  describe('Error Handling', () => {
    it('should correctly propagate 403 Forbidden errors', () => {
      service.getLicenses().subscribe({
        next: () => expect.fail('De request had moeten falen'),
        error: (error) => {
          expect(error.status).toBe(403);
        },
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/license'));
      req.flush('Forbidden', { status: 403, statusText: 'Forbidden' });
    });
  });
});
