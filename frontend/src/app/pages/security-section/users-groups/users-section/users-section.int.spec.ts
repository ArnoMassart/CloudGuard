import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { UsersSection } from './users-section';
import { UserService } from '../../../../services/user-service';
import { SecurityPreferencesFacade } from '../../../../services/security-preferences-facade';
import { SecurityScoreDetailService } from '../../../../services/security-score-detail.service';
import { USER_SECURITY_VIOLATION } from '../../../../models/users/UserOrgDetails';
import { provideTranslocoTesting } from '../../../../testing/transloco-testing';

const MOCK_OVERVIEW = {
  totalUsers: 100,
  withoutTwoFactor: 5,
  adminUsers: 2,
  securityScore: 92,
  warnings: {
    hasWarnings: true,
    items: { twoFactorWarning: true },
  },
};

const MOCK_USERS_PAGE = {
  users: [
    {
      fullName: 'Test Admin',
      email: 'test@admin.com',
      role: 'Super Admin',
      active: true,
      twoFactorEnabled: false, // Risico!
      securityConform: false,
      securityViolationCodes: [USER_SECURITY_VIOLATION.NO_2FA],
      lastLogin: '2024-03-25',
    },
  ],
  nextPageToken: 'token-123',
};

describe('UsersSection Integration', () => {
  let component: UsersSection;
  let fixture: ComponentFixture<UsersSection>;
  let httpTesting: HttpTestingController;
  let preferencesFacade: any;
  let securityScoreService: any;

  beforeEach(async () => {
    // 1. Voeg dit toe om de 'instantiated' error te voorkomen
    TestBed.resetTestingModule();

    preferencesFacade = {
      loadWithPrefs$: vi.fn((obs) => obs),
      isDisabled: vi.fn().mockReturnValue(false),
    };

    securityScoreService = {
      open: vi.fn(),
      createSimpleBreakdown: vi
        .fn()
        .mockReturnValue({ totalScore: 92, status: 'good', factors: [] }),
    };

    await TestBed.configureTestingModule({
      imports: [UsersSection],
      providers: [
        UserService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: SecurityPreferencesFacade, useValue: preferencesFacade },
        { provide: SecurityScoreDetailService, useValue: securityScoreService },
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersSection);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  const flushInitialRequests = () => {
    fixture.detectChanges(); // Trigger ngOnInit
    httpTesting.expectOne((r) => r.url.endsWith('/google/users/overview')).flush(MOCK_OVERVIEW);
    httpTesting.expectOne((r) => r.url.endsWith('/google/users')).flush(MOCK_USERS_PAGE);
    fixture.detectChanges();
  };

  it('should render the user table and overview cards', () => {
    flushInitialRequests();

    expect(component.orgUsers().length).toBe(1);

    const cards = fixture.debugElement.queryAll(By.css('app-section-top-card'));
    expect(cards[0].componentInstance.Value).toBe(100); // totalUsers

    const row = fixture.debugElement.query(By.css('tbody tr')).nativeElement;
    expect(row.textContent).toContain('Test Admin');
    expect(row.textContent).toContain('Super Admin');
  });

  describe('Security Muting Integration (The "Effective Conform" Logic)', () => {
    it('should show a user as "Not Conform" when 2FA is off and not muted', () => {
      // Preference staat op 'false' (niet uitgeschakeld)
      preferencesFacade.isDisabled.mockReturnValue(false);
      flushInitialRequests();

      const user = component.orgUsers()[0];
      expect(component.effectiveSecurityConform(user)).toBe(false);

      const securityCell = fixture.debugElement.queryAll(By.css('tbody td'))[5].nativeElement;
      expect(securityCell.textContent).toContain('Security Not conform');
      expect(securityCell.querySelector('.text-destructive')).toBeTruthy();
    });

    it('should show a user as "Conform" when 2FA is off but the 2FA warning is muted in preferences', () => {
      // Mock dat 2FA waarschuwingen zijn uitgeschakeld door de admin
      preferencesFacade.isDisabled.mockImplementation(
        (_section: any, key: string) => key === '2fa'
      );

      flushInitialRequests();

      const user = component.orgUsers()[0];
      // De logica moet nu 'true' teruggeven omdat het enige risico (2FA) genegeerd mag worden
      expect(component.effectiveSecurityConform(user)).toBe(true);

      const securityCell = fixture.debugElement.queryAll(By.css('tbody td'))[5].nativeElement;
      expect(securityCell.textContent).toContain('Conform');
      expect(securityCell.querySelector('.text-emerald-400')).toBeTruthy();
    });
  });

  it('should trigger a search and reset pagination when onSearch is called', () => {
    flushInitialRequests();
    const paginationResetSpy = vi.spyOn(component.pagination()!, 'reset');

    component.onSearch('John');
    fixture.detectChanges();

    const req = httpTesting.expectOne(
      (req) => req.url.endsWith('/google/users') && req.params.get('query') === 'John'
    );
    expect(req.request.method).toBe('GET');
    expect(paginationResetSpy).toHaveBeenCalled();

    req.flush(MOCK_USERS_PAGE);
  });

  it('should reload both overview and users when the refresh button is clicked', () => {
    flushInitialRequests();

    component.refreshData();
    expect(component.isRefreshing()).toBe(true);

    // 1. Handel de POST refresh af
    httpTesting.expectOne((r) => r.url.endsWith('/google/users/refresh')).flush('OK');

    // 2. Daarna komen de automatische GET calls weer door ngOnInit/langChanges of handmatige refresh
    httpTesting.expectOne((r) => r.url.endsWith('/google/users/overview')).flush(MOCK_OVERVIEW);
    httpTesting.expectOne((r) => r.url.endsWith('/google/users')).flush(MOCK_USERS_PAGE);

    fixture.detectChanges();
    expect(component.isRefreshing()).toBe(false);
  });
});
