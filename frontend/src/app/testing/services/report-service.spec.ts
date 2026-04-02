import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ReportService } from '../../services/report-service'; // Pas het pad aan indien nodig

describe('ReportService', () => {
  let service: ReportService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReportService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReportService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('downloadSecurityRapport', () => {
    it('GETs the report as a blob', () => {
      // Maak een neppe Blob aan om als response te dienen
      const mockBlob = new Blob(['dummy report content'], { type: 'application/pdf' });

      service.downloadSecurityRapport().subscribe((res) => {
        expect(res).toEqual(mockBlob);
        expect(res instanceof Blob).toBe(true);
      });

      const req = httpMock.expectOne((r) => r.url.endsWith('/report'));
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');

      req.flush(mockBlob);
    });
  });
});
