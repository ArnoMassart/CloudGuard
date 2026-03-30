import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { DnsRecordResponse } from '../../models/dns/DnsRecordResponse';
import { DnsService } from '../../services/dns-service';

describe('DnsService', () => {
  let service: DnsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DnsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DnsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDnsRecords', () => {
    it('GETs records with domain param and credentials', () => {
      const body: DnsRecordResponse = {
        domain: 'example.com',
        rows: [],
        securityScore: 100,
      };
      service.getDnsRecords('example.com').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) =>
          r.url === '/api/google/dns-records/records' && r.params.get('domain') === 'example.com',
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });
});
