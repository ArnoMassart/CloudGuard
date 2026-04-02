import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { SharedDrives } from './shared-drives';
import { DriveService } from '../../../services/drive-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SharedDrive } from '../../../models/drives/SharedDrive';
import { SharedDriveOverviewResponse } from '../../../models/drives/SharedDriveOverviewResponse';

// Mock vertalingen
const I18N_MOCK: Record<string, string> = {
  'shared-drives.title': 'Gedeelde Drives',
};

class SharedDrivesTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('SharedDrives', () => {
  let component: SharedDrives;
  let fixture: ComponentFixture<SharedDrives>;
  let translocoService: TranslocoService;

  // Mocks
  let driveServiceMock: {
    getDrives: ReturnType<typeof vi.fn>;
    getDrivesPageOverview: ReturnType<typeof vi.fn>;
    refreshDriveCache: ReturnType<typeof vi.fn>;
  };
  let securityScoreDetailMock: {
    createSimpleBreakdown: ReturnType<typeof vi.fn>;
    open: ReturnType<typeof vi.fn>;
  };
  let prefsFacadeMock: {
    isDisabled: ReturnType<typeof vi.fn>;
    loadWithPrefs$: ReturnType<typeof vi.fn>;
  };

  const mockDrives: SharedDrive[] = [
    {
      id: 'drive1',
      name: 'Marketing Drive',
      totalMembers: 5,
      externalMembers: 1,
      totalOrganizers: 2, // Gecorrigeerd van organizerCount naar totalOrganizers
      createdTime: '2024-01-01T10:00:00Z',
      parsedTime: '01-01-2024',
      onlyDomainUsersAllowed: false,
      onlyMembersCanAccess: true,
      risk: 'high',
    },
    {
      id: 'drive2',
      name: 'Finance Drive',
      totalMembers: 3,
      externalMembers: 0,
      totalOrganizers: 1,
      createdTime: '2024-02-15T14:30:00Z',
      parsedTime: '15-02-2024',
      onlyDomainUsersAllowed: true,
      onlyMembersCanAccess: true,
      risk: 'low',
    },
  ];

  const mockPageRes = {
    drives: mockDrives,
    nextPageToken: 'token_xyz',
  };

  const mockOverviewRes: SharedDriveOverviewResponse = {
    totalSharedDrives: 10,
    externalMembersDriveCount: 3,
    orphanDrives: 1,
    securityScore: 75,
    warnings: {
      hasWarnings: true,
      hasMultipleWarnings: true,
      items: {
        externalMembersWarning: true,
        orphanDrivesWarning: true,
        notOnlyDomainUsersAllowedWarning: false,
        notOnlyMembersCanAccessWarning: false,
      },
    },
    securityScoreBreakdown: undefined,
  } as unknown as SharedDriveOverviewResponse;

  beforeEach(async () => {
    driveServiceMock = {
      getDrives: vi.fn(() => of(mockPageRes)),
      getDrivesPageOverview: vi.fn(() => of(mockOverviewRes)),
      refreshDriveCache: vi.fn(() => of('OK')),
    };

    securityScoreDetailMock = {
      createSimpleBreakdown: vi.fn(() => ({ score: 75, factors: [] })),
      open: vi.fn(),
    };

    prefsFacadeMock = {
      isDisabled: vi.fn((category, key) => key === 'orphan'), // Orphan is disabled voor test
      loadWithPrefs$: vi.fn((obs) => obs),
    };

    await TestBed.configureTestingModule({
      imports: [SharedDrives],
      providers: [
        { provide: DriveService, useValue: driveServiceMock },
        { provide: SecurityScoreDetailService, useValue: securityScoreDetailMock },
        { provide: SecurityPreferencesFacade, useValue: prefsFacadeMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: SharedDrivesTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SharedDrives);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads overview and drives on init', () => {
    expect(prefsFacadeMock.loadWithPrefs$).toHaveBeenCalled();
    expect(driveServiceMock.getDrives).toHaveBeenCalledWith(2, undefined, '');

    expect(component.pageOverview()).toEqual(mockOverviewRes);
    expect(component.drives().length).toBe(2);
    // Controleert of de extra mapping (isLoadingDetails) heeft plaatsgevonden
    expect((component.drives()[0] as any).isLoadingDetails).toBe(true);
    expect(component.nextPageToken()).toBe('token_xyz');
  });

  it('computes warning signals and KPI colors correctly', () => {
    expect(component.hasWarnings()).toBe(true);
    expect(component.hasMultipleWarnings()).toBe(true);

    const warnings = component.drivePageWarnings();
    expect(warnings.externalMembersWarning).toBe(true);
    expect(warnings.orphanDrivesWarning).toBe(true);

    // KPI Kleuren (Orphan is gemockt als disabled, dus moet gedimd zijn)
    expect(component.kpiOrphanDrivesColors().text).toContain('#6b7280');
    // External is niet disabled, dus moet alert kleur hebben (omdat count > 0)
    expect(component.kpiExternalDrivesColors().bg).toContain('#ffedd4');
  });

  it('toggleExpanded updates isExpanded signal', () => {
    const initial = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!initial);
  });

  it('onSearch updates query and reloads data', () => {
    driveServiceMock.getDrives.mockClear();
    component.onSearch('project x');

    expect(component.searchQuery()).toBe('project x');
    expect(driveServiceMock.getDrives).toHaveBeenCalledWith(2, undefined, 'project x');
  });

  it('isSharedDrivePrefDisabled checks facade', () => {
    component.isSharedDrivePrefDisabled('outsideDomain');
    expect(prefsFacadeMock.isDisabled).toHaveBeenCalledWith('shared-drives', 'outsideDomain');
  });

  describe('openSecurityScoreDetail', () => {
    it('opens detail with simple breakdown if none exists', () => {
      component.openSecurityScoreDetail();
      expect(securityScoreDetailMock.createSimpleBreakdown).toHaveBeenCalledWith(75, 'drives');
      expect(securityScoreDetailMock.open).toHaveBeenCalled();
    });

    it('uses existing breakdown if present in overview', () => {
      const existingBreakdown = { score: 99, factors: [] };
      component.pageOverview.set({
        ...mockOverviewRes,
        securityScoreBreakdown: existingBreakdown,
      } as any);

      component.openSecurityScoreDetail();
      expect(securityScoreDetailMock.open).toHaveBeenCalledWith(existingBreakdown, 'drives');
    });
  });

  describe('refreshData', () => {
    it('triggers cache refresh and reloads all data', () => {
      driveServiceMock.getDrives.mockClear();
      component.refreshData();

      expect(driveServiceMock.refreshDriveCache).toHaveBeenCalled();
      expect(driveServiceMock.getDrives).toHaveBeenCalled();
      expect(component.isRefreshing()).toBe(false);
    });

    it('handles refresh error', () => {
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      driveServiceMock.refreshDriveCache.mockReturnValue(
        throwError(() => new Error('Refresh error'))
      );

      component.refreshData();

      expect(consoleSpy).toHaveBeenCalledWith('Kon cache niet vernieuwen:', expect.any(Error));
      expect(component.isRefreshing()).toBe(false);
      consoleSpy.mockRestore();
    });
  });

  it('handles loadDrives API error', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    driveServiceMock.getDrives.mockReturnValue(throwError(() => new Error('Network error')));

    translocoService.setActiveLang('nl'); // Trigger herladen
    await fixture.whenStable();

    expect(component.apiError()).toBe(true);
    expect(component.isLoading()).toBe(false);
    consoleSpy.mockRestore();
  });

  it('unsubscribes on destroy', () => {
    component.ngOnDestroy();
    driveServiceMock.getDrives.mockClear();

    translocoService.setActiveLang('nl');
    expect(driveServiceMock.getDrives).not.toHaveBeenCalled();
  });
});
