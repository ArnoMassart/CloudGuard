import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { WarmupCacheService } from '../../services/warmup-cache-service'; // Pas het pad aan indien nodig

describe('WarmupCacheService', () => {
  let service: WarmupCacheService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WarmupCacheService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(WarmupCacheService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('triggerWarmup', () => {
    it('POSTs to the warmup endpoint and logs success message', () => {
      // In Vitest gebruik je vi.spyOn()
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});

      service.triggerWarmup();

      const req = httpMock.expectOne((r) => r.url.endsWith('/cache-warmup'));
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ withCredentials: true });

      req.flush(null);

      // Controleer de actie
      expect(logSpy).toHaveBeenCalledWith(
        'Backend is op de achtergrond de data aan het inladen...'
      );

      // Netjes opruimen
      logSpy.mockRestore();
    });

    it('logs an error message if the warmup request fails', () => {
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      service.triggerWarmup();

      const req = httpMock.expectOne((r) => r.url.endsWith('/cache-warmup'));

      const mockError = new ProgressEvent('error');
      req.error(mockError);

      // In Vitest (net als in Jest) gebruik je expect.any()
      expect(errorSpy).toHaveBeenCalledWith('Warmup trigger mislukt', expect.any(Object));

      errorSpy.mockRestore();
    });
  });
});
