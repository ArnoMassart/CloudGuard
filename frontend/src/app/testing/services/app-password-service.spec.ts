import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppPassword } from '../../models/app-password/AppPassword';
import { AppPasswordOverviewResponse } from '../../models/app-password/AppPasswordOverviewResponse';
import { AppPasswordPageResponse } from '../../models/app-password/AppPasswordPageResponse';
import { AppPasswordsService } from '../../services/app-password-service';

describe('AppPasswordsService', () => {
  let service: AppPasswordsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [AppPasswordsService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AppPasswordsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAppPasswords', () => {
    it('sends size and omits pageToken and query when not needed', () => {
      const body: AppPasswordPageResponse = { users: [], nextPageToken: null };
      service.getAppPasswords(4).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) => r.url === '/api/google/app-passwords' && r.params.get('size') === '4',
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('pageToken')).toBe(false);
      expect(req.request.params.has('query')).toBe(false);
      req.flush(body);
    });

    it('sends pageToken and trims query', () => {
      const pw: AppPassword = {
        codeId: 1,
        name: 'Mail',
        creationTime: null,
        lastTimeUsed: null,
      };
      const body: AppPasswordPageResponse = {
        users: [
          {
            id: '1',
            name: 'A',
            email: 'a@b.com',
            role: 'USER',
            tsv: true,
            passwords: [pw],
          },
        ],
        nextPageToken: 'tok',
      };
      service.getAppPasswords(4, 'abc', '  q  ').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/app-passwords');
      expect(req.request.params.get('size')).toBe('4');
      expect(req.request.params.get('pageToken')).toBe('abc');
      expect(req.request.params.get('query')).toBe('q');
      req.flush(body);
    });

    it('omits query param when search is blank', () => {
      const body: AppPasswordPageResponse = { users: [], nextPageToken: null };
      service.getAppPasswords(4, undefined, '   ').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/app-passwords');
      expect(req.request.params.has('query')).toBe(false);
      req.flush(body);
    });
  });

  describe('getOverview', () => {
    it('GETs overview with credentials', () => {
      const body: AppPasswordOverviewResponse = {
        allowed: false,
        totalAppPasswords: 3,
        securityScore: 70,
      };
      service.getOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/app-passwords/overview');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshCache', () => {
    it('POSTs refresh with credentials and text response', () => {
      service.refreshCache().subscribe((text) => expect(text).toBe('done'));

      const req = httpMock.expectOne((r) => r.url === '/api/google/app-passwords/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.responseType).toBe('text');
      expect(req.request.body).toEqual({});
      req.flush('done');
    });
  });
});
