import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GroupOrgDetail } from '../../models/groups/GroupOrgDetail';
import {
  GroupService,
  GroupOverviewResponse,
  GroupPageResponse,
} from '../../services/group-service';

describe('GroupService', () => {
  let service: GroupService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [GroupService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(GroupService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getGroupsOverview', () => {
    it('GETs overview with credentials', () => {
      const body: GroupOverviewResponse = {
        totalGroups: 3,
        groupsWithExternal: 1,
        highRiskGroups: 0,
        mediumRiskGroups: 1,
        lowRiskGroups: 2,
        securityScore: 88,
      };
      service.getGroupsOverview().subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/groups/overview');
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      req.flush(body);
    });
  });

  describe('getOrgGroups', () => {
    it('sends size and omits query/pageToken when not provided', () => {
      const body: GroupPageResponse = { groups: [], nextPageToken: null };
      service.getOrgGroups(undefined, undefined, 5).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne(
        (r) => r.url === '/api/google/groups' && r.params.get('size') === '5',
      );
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBe(true);
      expect(req.request.params.has('query')).toBe(false);
      expect(req.request.params.has('pageToken')).toBe(false);
      req.flush(body);
    });

    it('trims and sends query param', () => {
      const sampleGroup: GroupOrgDetail = {
        name: 'team@example.com',
        adminId: 'admin',
        risk: 'LOW',
        tags: [],
        totalMembers: 1,
        externalMembers: 0,
        externalAllowed: false,
        whoCanJoin: 'CAN_REQUEST_TO_JOIN',
        whoCanView: 'ALL_IN_DOMAIN_CAN_VIEW',
      };
      const body: GroupPageResponse = { groups: [sampleGroup], nextPageToken: 'next' };
      service.getOrgGroups('  sales ', undefined, 2).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/groups');
      expect(req.request.params.get('size')).toBe('2');
      expect(req.request.params.get('query')).toBe('sales');
      req.flush(body);
    });

    it('sends pageToken when provided', () => {
      const body: GroupPageResponse = { groups: [], nextPageToken: null };
      service.getOrgGroups(undefined, 'abc123', 3).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/groups');
      expect(req.request.params.get('pageToken')).toBe('abc123');
      req.flush(body);
    });

    it('omits query param for blank search', () => {
      const body: GroupPageResponse = { groups: [], nextPageToken: null };
      service.getOrgGroups('   ', undefined, 5).subscribe((res) => expect(res).toEqual(body));

      const req = httpMock.expectOne((r) => r.url === '/api/google/groups');
      expect(req.request.params.has('query')).toBe(false);
      req.flush(body);
    });
  });

  describe('refreshGroupCache', () => {
    it('POSTs to refresh and reads text response', () => {
      service.refreshGroupCache().subscribe((text) => expect(text).toBe('OK'));

      const req = httpMock.expectOne((r) => r.url === '/api/google/groups/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.responseType).toBe('text');
      req.flush('OK');
    });
  });
});
