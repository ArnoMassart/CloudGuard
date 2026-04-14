import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DevicePageResponse } from '../../models/devices/DevicePageResponse';
import { DevicesOverviewResponse } from '../../models/devices/DevicesOverviewResponse';
import { DeviceService } from '../../services/device-service';

describe('DeviceService Integration', () => {
  let service: DeviceService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DeviceService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DeviceService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('getDevices (Query Parameters)', () => {
    it('should correctly map arguments to HttpParams', () => {
      const mockResponse = { devices: [], nextPageToken: null } as DevicePageResponse;

      service.getDevices('token123', 'COMPLIANT', 'ANDROID', 10).subscribe((res) => {
        expect(res).toEqual(mockResponse);
      });

      // Controleer of de URL klopt en de params aanwezig zijn
      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices'));

      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('pageToken')).toBe('token123');
      expect(req.request.params.get('status')).toBe('COMPLIANT');
      // Cruciaal: check of 'type' correct is gemapped naar 'deviceType'
      expect(req.request.params.get('deviceType')).toBe('ANDROID');
      expect(req.request.withCredentials).toBe(true);

      req.flush(mockResponse);
    });

    it('should use default size when no size is provided', () => {
      service.getDevices().subscribe();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices'));
      expect(req.request.params.get('size')).toBe('5');
      req.flush({});
    });
  });

  describe('getUniqueDeviceTypes', () => {
    it('should fetch a list of strings from the types endpoint', () => {
      const mockTypes = ['ANDROID', 'IOS', 'WINDOWS'];

      service.getUniqueDeviceTypes().subscribe((types) => {
        expect(types).toEqual(mockTypes);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices/types'));
      expect(req.request.method).toBe('GET');
      req.flush(mockTypes);
    });
  });

  describe('getDevicesPageOverview', () => {
    it('should fetch overview data', () => {
      const mockOverview = { totalDevices: 50, securityScore: 88 } as DevicesOverviewResponse;

      service.getDevicesPageOverview().subscribe((data) => {
        expect(data).toEqual(mockOverview);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices/overview'));
      expect(req.request.method).toBe('GET');
      req.flush(mockOverview);
    });
  });

  describe('refreshDeviceCache', () => {
    it('should POST to refresh and handle plain text response', () => {
      const mockMsg = 'Cache updated';

      service.refreshDeviceCache().subscribe((res) => {
        expect(res).toBe(mockMsg);
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      // In je huidige code stuur je dit mee in de body
      expect(req.request.body).toEqual({ withCredentials: true });

      req.flush(mockMsg);
    });
  });

  it('should handle API errors gracefully', () => {
    service.getUniqueDeviceTypes().subscribe({
      next: () => expect.fail('Should have failed with 404'),
      error: (error) => {
        expect(error.status).toBe(404);
      },
    });

    const req = httpTesting.expectOne((r) => r.url.endsWith('/google/devices/types'));
    req.flush('Not Found', { status: 404, statusText: 'Not Found' });
  });
});
