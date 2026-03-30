import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { GroupService } from '../../../../services/group-service';
import { SecurityPreferencesFacade } from '../../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../../services/security-score-detail.service';
import { GroupsSection } from './groups-section';

/** Keys used by groups-section template (avoids missing-translation noise in test output). */
const GROUPS_SECTION_I18N_STUB: Record<string, string> = {
  'groups.total-groups': 'Total groups',
  'groups.withExternal': 'With external',
  'groups.high-risk': 'High risk',
  'groups.withExternalDetected': 'External detected',
  'groups.highriskWarning': 'High risk warning',
  'groups.search-groups': 'Search groups',
  'groups.all': 'All',
  'groups.no-found': 'None',
  'groups.total-members': 'Members',
  'groups.external-members': 'External',
  'groups.external-allowed': 'External allowed',
  'groups.whoCanBeMember': 'Who can join',
  'groups.whoCanSeeMembers': 'Who can view',
  'groups.loading': 'Loading',
  'renew-data': 'Refresh',
  refreshing: 'Refreshing',
  page: 'Page',
  previous: 'Previous',
  next: 'Next',
  yes: 'Yes',
  no: 'No',
};

class GroupsSectionTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(GROUPS_SECTION_I18N_STUB);
  }
}

describe('GroupsSection', () => {
  let component: GroupsSection;
  let fixture: ComponentFixture<GroupsSection>;
  let groupServiceMock: {
    getGroupsOverview: ReturnType<typeof vi.fn>;
    getOrgGroups: ReturnType<typeof vi.fn>;
    refreshGroupCache: ReturnType<typeof vi.fn>;
  };
  let preferencesFacadeMock: {
    loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => import('rxjs').Observable<T>;
    isDisabled: ReturnType<typeof vi.fn>;
  };

  const overview = {
    totalGroups: 0,
    groupsWithExternal: 0,
    highRiskGroups: 0,
    mediumRiskGroups: 0,
    lowRiskGroups: 0,
    securityScore: 82,
  };

  beforeEach(async () => {
    groupServiceMock = {
      getGroupsOverview: vi.fn(() => of(overview)),
      getOrgGroups: vi.fn(() => of({ groups: [], nextPageToken: null })),
      refreshGroupCache: vi.fn(() => of('')),
    };
    preferencesFacadeMock = {
      loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => data$,
      isDisabled: vi.fn(() => false),
    };

    await TestBed.configureTestingModule({
      imports: [GroupsSection],
      providers: [
        { provide: GroupService, useValue: groupServiceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesFacadeMock },
        {
          provide: SecurityScoreDetailService,
          useValue: {
            open: vi.fn(),
            createSimpleBreakdown: vi.fn((score: number) => ({
              totalScore: score,
              status: score >= 75 ? 'good' : 'bad',
              factors: [
                {
                  title: 'overall-score',
                  description: 'score-detail.description.',
                  score,
                  maxScore: 100,
                  severity: 'success' as const,
                  muted: false,
                },
              ],
            })),
          },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: GroupsSectionTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupsSection);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads overview and first page on init', () => {
    expect(groupServiceMock.getGroupsOverview).toHaveBeenCalled();
    expect(groupServiceMock.getOrgGroups).toHaveBeenCalledWith(undefined, undefined, 2);
  });

  describe('getGroupAdminUrl', () => {
    it('returns default Google Admin URL when adminId is empty', () => {
      expect(
        component.getGroupAdminUrl({
          name: 'g',
          adminId: '   ',
          risk: 'LOW',
          tags: [],
          totalMembers: 0,
          externalMembers: 0,
          externalAllowed: false,
          whoCanJoin: '',
          whoCanView: '',
        }),
      ).toBe('https://admin.google.com/u/1/ac/groups');
    });

    it('returns group settings URL when adminId is set', () => {
      expect(
        component.getGroupAdminUrl({
          name: 'g',
          adminId: 'id/with space',
          risk: 'LOW',
          tags: [],
          totalMembers: 0,
          externalMembers: 0,
          externalAllowed: false,
          whoCanJoin: '',
          whoCanView: '',
        }),
      ).toBe('https://admin.google.com/u/1/ac/groups/id%2Fwith%20space/settings');
    });
  });

  it('onSearch resets paging and passes query through to the service', () => {
    groupServiceMock.getOrgGroups.mockClear();
    component.onSearch('  hello ');
    expect(groupServiceMock.getOrgGroups).toHaveBeenCalledWith('  hello ', undefined, 2);
    expect(component.currentPage()).toBe(1);
  });

  it('toggleExpanded flips isExpanded', () => {
    const initial = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!initial);
  });

  it('isGroupExternalPrefDisabled reads facade', () => {
    preferencesFacadeMock.isDisabled.mockReturnValue(true);
    expect(component.isGroupExternalPrefDisabled()).toBe(true);
    expect(preferencesFacadeMock.isDisabled).toHaveBeenCalledWith('users-groups', 'groupExternal');
  });

  it('nextPage requests the next token when present', () => {
    groupServiceMock.getOrgGroups.mockImplementation((_q, token: string | undefined) => {
      if (!token) {
        return of({ groups: [], nextPageToken: 'tok-next' });
      }
      return of({ groups: [], nextPageToken: null });
    });
    fixture = TestBed.createComponent(GroupsSection);
    component = fixture.componentInstance;
    fixture.detectChanges();

    groupServiceMock.getOrgGroups.mockClear();
    component.nextPage();
    expect(groupServiceMock.getOrgGroups).toHaveBeenCalledWith(undefined, 'tok-next', 2);
    expect(component.currentPage()).toBe(2);
  });

  it('prevPage goes back to previous token', () => {
    groupServiceMock.getOrgGroups.mockImplementation((_q, token: string | undefined) => {
      if (!token) {
        return of({ groups: [], nextPageToken: 'page2' });
      }
      return of({ groups: [], nextPageToken: null });
    });
    fixture = TestBed.createComponent(GroupsSection);
    component = fixture.componentInstance;
    fixture.detectChanges();
    component.nextPage();
    groupServiceMock.getOrgGroups.mockClear();
    component.prevPage();
    expect(groupServiceMock.getOrgGroups).toHaveBeenCalledWith(undefined, undefined, 2);
    expect(component.currentPage()).toBe(1);
  });

  it('openSecurityScoreDetail opens dialog with overview breakdown when present', () => {
    groupServiceMock.getGroupsOverview.mockReturnValue(
      of({
        ...overview,
        securityScoreBreakdown: {
          totalScore: 82,
          status: 'good',
          factors: [],
        },
      }),
    );
    fixture = TestBed.createComponent(GroupsSection);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const detail = TestBed.inject(SecurityScoreDetailService);
    component.openSecurityScoreDetail();
    expect(detail.open).toHaveBeenCalledWith(
      {
        totalScore: 82,
        status: 'good',
        factors: [],
      },
      'groups',
    );
  });

  it('refreshData calls refresh endpoint and reloads lists', () => {
    groupServiceMock.getGroupsOverview.mockClear();
    groupServiceMock.getOrgGroups.mockClear();
    component.refreshData();
    expect(groupServiceMock.refreshGroupCache).toHaveBeenCalled();
    expect(groupServiceMock.getOrgGroups).toHaveBeenCalled();
    expect(groupServiceMock.getGroupsOverview).toHaveBeenCalled();
    expect(component.isRefreshing()).toBe(false);
  });
});
