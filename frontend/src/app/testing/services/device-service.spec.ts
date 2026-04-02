import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DeviceService } from '../../services/device-service';
import { DevicePageResponse } from '../../models/devices/DevicePageResponse';
import { DevicesOverviewResponse } from '../../models/devices/DevicesOverviewResponse';

describe('DeviceService', () => {
  let service: DeviceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DeviceService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DeviceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDevices', () => {
    it('sends default size and omits other params when not provided', () => {
      const body = { devices: [], nextPageToken: null } as unknown as DevicePageResponse;
      service.getDevices().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) => r.url.endsWith('/google/devices') && r.params.get('size') === '5'
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('pageToken')).toBe(false);
      expect(req.request.params.has('status')).toBe(false);
      expect(req.request.params.has('deviceType')).toBe(false);
      req.flush(body);
    });

    it('sends all params when provided', () => {
      const body = { devices: [], nextPageToken: 'next123' } as unknown as DevicePageResponse;
      service
        .getDevices('page1', 'COMPLIANT', 'ANDROID', 10)
        .subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/devices'));
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('pageToken')).toBe('page1');
      expect(req.request.params.get('status')).toBe('COMPLIANT');
      expect(req.request.params.get('deviceType')).toBe('ANDROID');
      req.flush(body);
    });
  });

  describe('getUniqueDeviceTypes', () => {
    it('GETs unique device types with credentials', () => {
      const body = ['ANDROID', 'IOS', 'CHROME_OS'];
      service.getUniqueDeviceTypes().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/devices/types'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getDevicesPageOverview', () => {
    it('GETs device overview with credentials', () => {
      const body = {
        totalDevices: 50,
        totalNonCompliant: 5,
        securityScore: 90,
      } as DevicesOverviewResponse;

      service.getDevicesPageOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/devices/overview'));
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshDeviceCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshDeviceCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url.endsWith('/google/devices/refresh'));
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({ withCredentials: true });
      req.flush('OK');
    });
  });
});
