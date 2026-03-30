import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Domain } from '../../models/domain/Domain';
import { DomainService } from '../../services/domain-service';

describe('DomainService', () => {
  let service: DomainService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DomainService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(DomainService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getDomains', () => {
    it('GETs domains with credentials', () => {
      const body: Domain[] = [
        { domainName: 'a.com', domainType: 'Primary Domain', isVerified: true },
      ];
      service.getDomains().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/domains');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshCache', () => {
    it('POSTs refresh with credentials and text response', () => {
      service.refreshCache().subscribe((text) => expect(text).toBe('ok'));

      const req = httpMock.expectOne((r) => r.url === '/api/google/domains/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({});
      req.flush('ok');
    });
  });
});
