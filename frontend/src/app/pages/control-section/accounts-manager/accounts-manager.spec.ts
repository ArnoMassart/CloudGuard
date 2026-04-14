import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader, TranslocoService } from '@jsverse/transloco';
import { of, throwError } from 'rxjs';
import { AccountsManager } from './accounts-manager';
import { UserService } from '../../../services/user-service';
import { MatDialog } from '@angular/material/dialog';
import { Role } from '../../../models/users/User';

// Mock vertalingen
const FB_I18N: Record<string, string> = {
  'accounts-manager': 'Account Manager',
  'users.search': 'Zoeken',
};

class AccountsManagerTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(FB_I18N);
  }
}

describe('AccountsManager', () => {
  let component: AccountsManager;
  let fixture: ComponentFixture<AccountsManager>;
  let translocoService: TranslocoService;

  let userServiceMock: {
    getAllDatabaseUsers: ReturnType<typeof vi.fn>;
    getAllDatabaseUsersWithoutRoles: ReturnType<typeof vi.fn>;
    updateRolesForUser: ReturnType<typeof vi.fn>;
    updateRolesForUserWithoutAny: ReturnType<typeof vi.fn>;
  };

  let matDialogMock: {
    open: ReturnType<typeof vi.fn>;
  };

  const mockUsers = [
    { email: 'user1@cloudmen.com', firstName: 'John', lastName: 'Doe', roles: [Role.SUPER_ADMIN] },
  ];

  const mockPageRes = {
    users: mockUsers,
    nextPageToken: 'page2',
  };

  beforeEach(async () => {
    // Definieer de mocks voor Vitest
    userServiceMock = {
      getAllDatabaseUsers: vi.fn(() => of(mockPageRes)),
      getAllDatabaseUsersWithoutRoles: vi.fn(() => of({ users: [], nextPageToken: null })),
      updateRolesForUser: vi.fn(() => of({})),
      updateRolesForUserWithoutAny: vi.fn(() => of({})),
    };

    matDialogMock = {
      open: vi.fn(() => ({ afterClosed: () => of(null) })),
    };

    await TestBed.configureTestingModule({
      imports: [AccountsManager],
      providers: [
        { provide: UserService, useValue: userServiceMock },
        { provide: MatDialog, useValue: matDialogMock },
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
            reRenderOnLangChange: true,
          },
          loader: AccountsManagerTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManager);
    component = fixture.componentInstance;
    translocoService = TestBed.inject(TranslocoService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('loads users and maps to signals correctly on init', () => {
    expect(userServiceMock.getAllDatabaseUsers).toHaveBeenCalled();
    expect(userServiceMock.getAllDatabaseUsersWithoutRoles).toHaveBeenCalled();

    expect(component.isLoading()).toBe(false);
    expect(component.users()).toEqual(mockUsers);
    expect(component.nextPageToken()).toBe('page2');

    expect(component.isLoadingWithoutRoles()).toBe(false);
    expect(component.usersWithoutRoles()).toEqual([]);
  });

  it('updates search query and resets pagination on search', () => {
    // Simuleer pagination reset (omdat we viewChild gebruiken moeten we een mock toepassen of eromheen testen)
    component.onSearch('Arno');

    expect(component.searchQuery()).toBe('Arno');
    expect(userServiceMock.getAllDatabaseUsers).toHaveBeenCalled(); // Wordt opnieuw aangeroepen
  });

  it('toggleRolesSummary flips the state in the Set correctly', () => {
    const event = new Event('click');
    event.stopPropagation = vi.fn();

    // Als de lengte niet > 2 is, gebeurt er niets
    component.toggleRolesSummary('user@test.com', 2, event);
    expect(component.expandedRoles().has('user@test.com')).toBe(false);

    // Als de lengte > 2 is, wordt hij toegevoegd
    component.toggleRolesSummary('user@test.com', 5, event);
    expect(component.expandedRoles().has('user@test.com')).toBe(true);

    // Tweede keer klikken verwijdert hem weer
    component.toggleRolesSummary('user@test.com', 5, event);
    expect(component.expandedRoles().has('user@test.com')).toBe(false);
  });

  it('getAllAvailableRoles returns sorted roles without UNASSIGNED', () => {
    const roles = component.getAllAvailableRoles();

    expect(roles.length).toBeGreaterThan(0);
    const hasUnassigned = roles.some((r) => r.value === Role.UNASSIGNED);
    expect(hasUnassigned).toBe(false);
  });

  it('handles loadUsers API error gracefully (logs to console)', async () => {
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    userServiceMock.getAllDatabaseUsers.mockReturnValue(throwError(() => new Error('API failed')));

    // Trigger de API call opnieuw via taalverandering
    translocoService.setActiveLang('nl');
    await fixture.whenStable();

    expect(consoleSpy).toHaveBeenCalledWith('Failed to load users', expect.any(Error));
    expect(component.isLoading()).toBe(false);

    consoleSpy.mockRestore();
  });

  it('cleans up language subscription on destroy', () => {
    component.ngOnDestroy();
    userServiceMock.getAllDatabaseUsers.mockClear();

    translocoService.setActiveLang('nl');

    expect(userServiceMock.getAllDatabaseUsers).not.toHaveBeenCalled();
  });
});
