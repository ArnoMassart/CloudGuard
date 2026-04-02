import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { AppPasswordsService } from '../../../services/app-password-service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { AppPasswords } from './app-passwords';

const APP_PASSWORDS_I18N: Record<string, string> = {
  'app-passwords.title': 'App passwords',
  allowed: 'Allowed',
  'app-passwords.total-top': 'Total',
  'app-passwords.warning.description': 'Warning',
  'users.search': 'Search',
  'app-passwords.all': 'All',
  refreshing: 'Refreshing',
  'renew-data': 'Refresh',
  'user-cap': 'User',
  role: 'Role',
  'app-password-dash': 'App passwords',
  created: 'Created',
  'last-used': 'Last used',
  never: 'Never',
  'app-passwords.none': 'None',
  page: 'Page',
  previous: 'Previous',
  next: 'Next',
  'app-passwords.load-error': 'Error',
  'app-passwords.error.overview-failed': 'Overview failed',
  'app-passwords.error.list-failed': 'List failed',
  'app-passwords.error.refresh-failed': 'Refresh failed',
  'app-passwords.no-users': 'No users',
  'try-again': 'Retry',
  'app-passwords.loading': 'Loading',
  'to-admin-console': 'Admin',
};

class AppPasswordsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(APP_PASSWORDS_I18N);
  }
}

describe('AppPasswords', () => {
  let component: AppPasswords;
  let fixture: ComponentFixture<AppPasswords>;
  let serviceMock: {
    getOverview: ReturnType<typeof vi.fn>;
    getAppPasswords: ReturnType<typeof vi.fn>;
    refreshCache: ReturnType<typeof vi.fn>;
  };
  let preferencesMock: {
    loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => import('rxjs').Observable<T>;
    isDisabled: ReturnType<typeof vi.fn>;
  };

  const overview = {
    allowed: true,
    totalAppPasswords: 2,
    securityScore: 88,
  };

  const sampleUsers = [
    {
      id: 'id-1',
      name: 'Alice',
      email: 'alice@ex.com',
      role: 'USER',
      tsv: true,
      passwords: [],
    },
    {
      id: 'id-2',
      name: 'Bob',
      email: 'bob@ex.com',
      role: 'ADMIN',
      tsv: false,
      passwords: [{ codeId: 1, name: 'Legacy', creationTime: null, lastTimeUsed: null }],
    },
  ];

  beforeEach(async () => {
    serviceMock = {
      getOverview: vi.fn(() => of(overview)),
      getAppPasswords: vi.fn(() => of({ users: sampleUsers, nextPageToken: null })),
      refreshCache: vi.fn(() => of('')),
    };
    preferencesMock = {
      loadWithPrefs$: <T>(data$: import('rxjs').Observable<T>) => data$,
      isDisabled: vi.fn(() => false),
    };

    await TestBed.configureTestingModule({
      imports: [AppPasswords],
      providers: [
        { provide: AppPasswordsService, useValue: serviceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesMock },
        {
          provide: SecurityScoreDetailService,
          useValue: {
            open: vi.fn(),
            createSimpleBreakdown: vi.fn((score: number, subtitle: string) => ({
              totalScore: score,
              status: 'good',
              factors: [{ title: 'x', description: 'y', score, maxScore: 100, severity: 'success' as const, muted: false }],
            })),
          },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: AppPasswordsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads overview and first page on init', () => {
    expect(serviceMock.getOverview).toHaveBeenCalled();
    expect(serviceMock.getAppPasswords).toHaveBeenCalledWith(4, undefined, '');
    expect(component.userAppPasswords().length).toBe(2);
    expect(component.isLoading()).toBe(false);
  });

  describe('getAdminUserUrl', () => {
    it('returns default users URL when id is empty', () => {
      expect(
        component.getAdminUserUrl({
          id: '  ',
          name: '',
          email: 'e',
          role: '',
          twoFactorEnabled: false,
          appPasswords: [],
        }),
      ).toBe('https://admin.google.com/u/1/ac/users');
    });

    it('returns user admin URL when id is set', () => {
      expect(
        component.getAdminUserUrl({
          id: 'user/1',
          name: '',
          email: 'e',
          role: '',
          twoFactorEnabled: false,
          appPasswords: [],
        }),
      ).toBe('https://admin.google.com/u/1/ac/users/user%2F1');
    });
  });

  it('onSearch resets paging and reloads with query', () => {
    serviceMock.getAppPasswords.mockClear();
    component.onSearch('foo');
    expect(serviceMock.getAppPasswords).toHaveBeenCalledWith(4, undefined, 'foo');
    expect(component.currentPage()).toBe(1);
  });

  it('toggleExpanded flips isExpanded', () => {
    const v = component.isExpanded();
    component.toggleExpanded();
    expect(component.isExpanded()).toBe(!v);
  });

  it('toggleExpand selects and clears row', () => {
    component.toggleExpand('alice@ex.com');
    expect(component.expandedAppPassword()).toBe('alice@ex.com');
    component.toggleExpand('alice@ex.com');
    expect(component.expandedAppPassword()).toBe(null);
  });

  it('filteredUserAppPasswords filters by name or email', () => {
    component.userAppPasswords.set([
      { id: '1', name: 'Alice X', email: 'a@b.com', role: 'U', twoFactorEnabled: true, appPasswords: [] },
      { id: '2', name: 'Zed', email: 'zed@c.com', role: 'U', twoFactorEnabled: true, appPasswords: [] },
    ]);
    component.searchQuery.set('zed');
    expect(component.filteredUserAppPasswords().length).toBe(1);
    expect(component.filteredUserAppPasswords()[0].email).toBe('zed@c.com');
  });

  it('nextPage and prevPage pass page tokens', () => {
    serviceMock.getAppPasswords.mockImplementation((_size, token?: string) => {
      if (!token) {
        return of({ users: sampleUsers, nextPageToken: 'next-tok' });
      }
      return of({ users: [], nextPageToken: null });
    });
    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    fixture.detectChanges();

    serviceMock.getAppPasswords.mockClear();
    component.nextPage();
    expect(serviceMock.getAppPasswords).toHaveBeenCalledWith(4, 'next-tok', '');
    expect(component.currentPage()).toBe(2);

    serviceMock.getAppPasswords.mockClear();
    component.prevPage();
    expect(serviceMock.getAppPasswords).toHaveBeenCalledWith(4, undefined, '');
    expect(component.currentPage()).toBe(1);
  });

  it('openSecurityScoreDetail uses overview breakdown when present', () => {
    serviceMock.getOverview.mockReturnValue(
      of({
        ...overview,
        securityScoreBreakdown: {
          totalScore: 88,
          status: 'good',
          factors: [],
        },
      }),
    );
    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    fixture.detectChanges();

    const detail = TestBed.inject(SecurityScoreDetailService);
    component.openSecurityScoreDetail();
    expect(detail.open).toHaveBeenCalledWith(
      { totalScore: 88, status: 'good', factors: [] },
      'app-passwords',
    );
  });

  it('refreshData refreshes cache and reloads data', () => {
    serviceMock.getAppPasswords.mockClear();
    serviceMock.getOverview.mockClear();
    component.refreshData();
    expect(serviceMock.refreshCache).toHaveBeenCalled();
    expect(serviceMock.getOverview).toHaveBeenCalled();
    expect(serviceMock.getAppPasswords).toHaveBeenCalled();
    expect(component.isRefreshing()).toBe(false);
  });

  it('sets loadError when list request fails', async () => {
    serviceMock.getAppPasswords.mockReturnValue(throwError(() => new Error('fail')));
    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.listLoadError()).toBeTruthy();
    expect(component.listLoadError() ?? '').toContain('fail');
    expect(component.userAppPasswords()).toEqual([]);
  });

  it('retryLoad resets tokens and reloads', () => {
    component.retryLoad();
    expect(serviceMock.getAppPasswords).toHaveBeenCalledWith(4, undefined, '');
    expect(component.currentPage()).toBe(1);
  });

  it('formatDate returns dash for empty or invalid', () => {
    expect(component.formatDate(null)).toBe('–');
    expect(component.formatDate('not-a-date')).toBe('–');
  });

  it('formatLastUsed returns nooit for empty or invalid', () => {
    expect(component.formatLastUsed(null)).toBe('nooit');
    expect(component.formatLastUsed('bad')).toBe('nooit');
  });

  it('formatLastUsed uses relative labels for recent dates', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-15T14:00:00.000Z'));
    expect(component.formatLastUsed(new Date('2026-06-15T12:00:00.000Z'))).toBe('vandaag');
    expect(component.formatLastUsed(new Date('2026-06-14T12:00:00.000Z'))).toBe('gisteren');
    expect(component.formatLastUsed(new Date('2026-06-13T12:00:00.000Z'))).toBe('2 dagen geleden');
    vi.useRealTimers();
  });

  it('appPasswordAlertsEnabled is false when preference disables alerts', async () => {
    preferencesMock.isDisabled.mockReturnValue(true);
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AppPasswords],
      providers: [
        { provide: AppPasswordsService, useValue: serviceMock },
        { provide: SecurityPreferencesFacade, useValue: preferencesMock },
        {
          provide: SecurityScoreDetailService,
          useValue: { open: vi.fn(), createSimpleBreakdown: vi.fn() },
        },
        provideTransloco({
          config: {
            availableLangs: ['en'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: AppPasswordsTranslocoLoader,
        }),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.appPasswordAlertsEnabled()).toBe(false);
    expect(preferencesMock.isDisabled).toHaveBeenCalledWith('app-passwords', 'appPassword');
  });
});
