import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrgUnitPolicyDto } from '../../models/org-unit/OrgUnitPolicyDto';
import { OrgUnitNode } from '../../pages/security-section/organizational-units/organizational-units';
import { OrgUnitService } from '../../services/org-unit-service';

describe('OrgUnitService', () => {
  let service: OrgUnitService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [OrgUnitService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrgUnitService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getOrgUnitTree', () => {
    it('GETs tree with credentials', () => {
      const body: OrgUnitNode = {
        id: 'root',
        name: 'Root',
        userCount: 0,
      };
      service.getOrgUnitTree().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/org-units');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getPoliciesForOrgUnit', () => {
    it('GETs policies with orgUnitPath query param', () => {
      const body: OrgUnitPolicyDto[] = [];
      service.getPoliciesForOrgUnit('/foo/bar').subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) =>
          r.url === '/api/google/org-units/policies' &&
          r.params.get('orgUnitPath') === '/foo/bar',
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('refreshOrgUnitsCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshOrgUnitsCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url === '/api/google/org-units/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      req.flush('OK');
    });
  });
});
