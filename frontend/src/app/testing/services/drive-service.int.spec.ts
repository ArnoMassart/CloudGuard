import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SharedDriveOverviewResponse } from '../../models/drives/SharedDriveOverviewResponse';
import { SharedDrivePageResponse } from '../../models/drives/SharedDrivePageResponse';
import { DriveService } from '../../services/drive-service';

describe('DriveService Integration', () => {
  let service: DriveService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DriveService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DriveService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verifieer dat er geen onverwachte of onafgemaakte HTTP-verzoeken zijn
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getDrives (Pagination & Search)', () => {
    it('should correctly append size, pageToken and query to the URL', () => {
      const mockResponse = {
        drives: [],
        nextPageToken: 'next-123',
      } as unknown as SharedDrivePageResponse;
      const size = 10;
      const token = 'token-abc';
      const query = 'marketing';

      service.getDrives(size, token, query).subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      // Verwacht één verzoek naar de basis-URL
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/drives'));

      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      // Controleer de opgebouwde HttpParams
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('pageToken')).toBe('token-abc');
      expect(req.request.params.get('query')).toBe('marketing');

      req.flush(mockResponse);
    });

    it('should only include size when other params are missing', () => {
      service.getDrives(5).subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/drives'));
      expect(req.request.params.has('size')).toBe(true);
      expect(req.request.params.has('pageToken')).toBe(false);
      expect(req.request.params.has('query')).toBe(false);

      req.flush({});
    });
  });

  describe('getDrivesPageOverview', () => {
    it('should fetch overview data from the correct endpoint', () => {
      const mockOverview: SharedDriveOverviewResponse = {
        totalDrives: 10,
        orphanDrives: 2,
        totalHighRisk: 1,
        totalExternalMembers: 5,
        securityScore: 78,
        notOnlyDomainUsersAllowedCount: 3,
        notOnlyMembersCanAccessCount: 1,
        externalMembersDriveCount: 4,
        securityScoreBreakdown: {
          totalScore: 78,
          status: 'good',
          factors: [],
        },
        warnings: {
          hasWarnings: true,
          hasMultipleWarnings: false,
          items: { orphanDrivesWarning: true },
        },
      };

      service.getDrivesPageOverview().subscribe((data) => {
        expect(data).toEqual(mockOverview);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/drives/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockOverview);
    });
  });

  describe('refreshDriveCache', () => {
    it('should POST to the refresh endpoint and expect a text response', () => {
      const mockMsg = 'Cache refresh triggered';

      service.refreshDriveCache().subscribe((res) => {
        expect(res).toBe(mockMsg);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/drives/refresh'));

      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      // Verifieer de body (waarin withCredentials momenteel als data wordt gestuurd)
      expect(req.request.body).toEqual({ withCredentials: true });

      req.flush(mockMsg);
    });
  });

  describe('HTTP Error Handling', () => {
    it('should bubble up server errors to the component', () => {
      service.getDrivesPageOverview().subscribe({
        next: () => expect.fail('De request had moeten falen'),
        error: (error) => {
          expect(error.status).toBe(503);
          expect(error.statusText).toBe('Service Unavailable');
        },
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/drives/overview'));
      req.flush('Service Unavailable', { status: 503, statusText: 'Service Unavailable' });
    });
  });
});
