import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReportService } from '../../services/report-service';

describe('ReportService Integration', () => {
  let service: ReportService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReportService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ReportService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verifieert dat er geen openstaande requests zijn
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('downloadSecurityRapport', () => {
    it('should perform a GET request with responseType "blob"', () => {
      // We maken een dummy Blob aan om de PDF-data te simuleren
      const mockBlob = new Blob(['fake-pdf-binary-content'], { type: 'application/pdf' });

      service.downloadSecurityRapport().subscribe((blob) => {
        expect(blob).toEqual(mockBlob);
        expect(blob.size).toBeGreaterThan(0);
        expect(blob.type).toBe('application/pdf');
      });

      // Controleer of de URL klopt
      const req = httpTesting.expectOne((r) => r.url.endsWith('/report'));

      expect(req.request.method).toBe('GET');

      // Cruciaal: check of de service expliciet om een blob vraagt
      expect(req.request.responseType).toBe('blob');

      // Geef de mock blob terug
      req.flush(mockBlob);
    });

    it('should propagate a 500 error if the server fails to generate the report', () => {
      service.downloadSecurityRapport().subscribe({
        next: () => expect.fail('De request had moeten falen met een 500 error'),
        error: (error) => {
          expect(error.status).toBe(500);
          expect(error.statusText).toBe('Internal Server Error');
        },
      });

      const req = httpTesting.expectOne((r) => r.url.endsWith('/report'));

      // FIX: Gebruik een (lege) Blob in plaats van een string
      const errorBody = new Blob([JSON.stringify({ message: 'Error generating report' })], {
        type: 'application/json',
      });

      req.flush(errorBody, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
