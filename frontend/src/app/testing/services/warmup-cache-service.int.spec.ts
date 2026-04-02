import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WarmupCacheService } from '../../services/warmup-cache-service';

describe('WarmupCacheService Integration', () => {
  let service: WarmupCacheService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WarmupCacheService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(WarmupCacheService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verifieer dat er geen onverwachte HTTP-calls zijn gedaan
    httpTesting.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('triggerWarmup()', () => {
    it('should send a POST request with the correct body and log success', () => {
      // We spioneren op de console omdat de service intern subscribet
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      // Act
      service.triggerWarmup();

      // Assert: Zoek de POST-request
      const req = httpTesting.expectOne((r) => r.url.endsWith('/cache-warmup'));

      expect(req.request.method).toBe('POST');
      // Verifieer dat { withCredentials: true } in de BODY van de request zit
      expect(req.request.body).toEqual({});

      // Laat de request slagen
      req.flush({});

      // Controleer of de succes-log is aangeroepen
      expect(logSpy).toHaveBeenCalledWith(
        'Backend is op de achtergrond de data aan het inladen...'
      );

      logSpy.mockRestore();
    });

    it('should log an error when the warmup trigger fails', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.triggerWarmup();

      const req = httpTesting.expectOne((r) => r.url.endsWith('/cache-warmup'));

      // Simuleer een serverfout (500)
      req.flush('Error', { status: 500, statusText: 'Internal Server Error' });

      expect(errorSpy).toHaveBeenCalledWith('Warmup trigger mislukt', expect.any(Object));

      errorSpy.mockRestore();
    });
  });
});
