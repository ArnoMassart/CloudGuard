import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DriveService } from '../../services/drive-service';
import { SharedDrivePageResponse } from '../../models/drives/SharedDrivePageResponse';
import { SharedDriveOverviewResponse } from '../../models/drives/SharedDriveOverviewResponse';

describe('DriveService', () => {
  let service: DriveService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DriveService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DriveService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDrives', () => {
    it('sends size and omits query/pageToken when not provided', () => {
      const body = { drives: [], nextPageToken: '' } as unknown as SharedDrivePageResponse;
      service.getDrives(10).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) => r.url.endsWith('/google/drives') && r.params.get('size') === '10'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('query')).toBe(false);
      expect(req.request.params.has('pageToken')).toBe(false);
      req.flush(body);
    });

    it('sends pageToken and query when provided', () => {
      const body = { drives: [], nextPageToken: 'next123' } as unknown as SharedDrivePageResponse;
      service.getDrives(5, 'page1', 'marketing').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/drives'));
      expect(req.request.params.get('size')).toBe('5');
      expect(req.request.params.get('pageToken')).toBe('page1');
      expect(req.request.params.get('query')).toBe('marketing');
      req.flush(body);
    });
  });

  describe('getDrivesPageOverview', () => {
    it('GETs overview with credentials', () => {
      const body = {
        totalSharedDrives: 15,
        highRiskDrives: 2,
        securityScore: 85,
      } as unknown as SharedDriveOverviewResponse;

      service.getDrivesPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/drives/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshDriveCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshDriveCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/drives/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({ withCredentials: true });
      req.flush('OK');
    });
  });
});
