import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TranslocoService } from '@jsverse/transloco';
import { By } from '@angular/platform-browser';
import { AccountsManager } from './accounts-manager';
import { UserService } from '../../../services/user-service';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';
import { Role } from '../../../models/users/User';

describe('AccountsManager Integration', () => {
  let component: AccountsManager;
  let fixture: ComponentFixture<AccountsManager>;
  let httpTesting: HttpTestingController;
  let translocoService: TranslocoService;

  const mockUsers = [
    {
      email: 'admin@cloudmen.com',
      firstName: 'Arno',
      lastName: 'Massart',
      roles: [Role.SUPER_ADMIN],
    },
    {
      email: 'many@cloudmen.com',
      firstName: 'Test',
      lastName: 'User',
      roles: [Role.ORG_UNITS_VIEWER, Role.USERS_GROUPS_VIEWER, Role.LICENSES_VIEWER],
    },
  ];

  beforeEach(async () => {
    TestBed.resetTestingModule();

    await TestBed.configureTestingModule({
      imports: [AccountsManager],
      providers: [
        UserService, // De ECHTE service
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTranslocoTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManager);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    translocoService = TestBed.inject(TranslocoService);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should render data from the API in the tables', () => {
    // 1. Trigger ngOnInit
    fixture.detectChanges();

    // 2. Handel de HTTP requests af
    const withoutRolesReq = httpTesting.expectOne((r) => r.url.endsWith('/user/all/no-roles'));
    const allUsersReq = httpTesting.expectOne((r) => r.url.endsWith('/user/all'));

    withoutRolesReq.flush({ users: [], nextPageToken: null });
    allUsersReq.flush({ users: mockUsers, nextPageToken: null });

    // 3. Update UI
    fixture.detectChanges();

    // Assert: Tabel met alle gebruikers moet 2 rijen bevatten
    const tableRows = fixture.debugElement
      .queryAll(By.css('table'))
      .pop()
      ?.queryAll(By.css('tbody tr'));
    expect(tableRows?.length).toBe(2);
    expect(tableRows?.[0].nativeElement.textContent).toContain('Arno Massart');

    // Assert: "Without roles" sectie moet verborgen zijn omdat de API [] teruggaf
    const withoutRolesDiv = fixture.debugElement.query(By.css('.hidden'));
    expect(withoutRolesDiv).toBeTruthy();
  });

  it('should truncate roles and show a +X badge when a user has more than 2 roles', () => {
    fixture.detectChanges();

    httpTesting
      .expectOne((r) => r.url.endsWith('/user/all/no-roles'))
      .flush({ users: [], nextPageToken: null });
    httpTesting
      .expectOne((r) => r.url.endsWith('/user/all'))
      .flush({ users: mockUsers, nextPageToken: null });

    fixture.detectChanges();

    // Zoek de rij van de gebruiker met veel rollen
    const rowContent = fixture.debugElement.queryAll(By.css('tbody tr'))[1].nativeElement
      .textContent;

    // Omdat er 3 rollen zijn, moet het truncaten en een "+1" tonen
    expect(rowContent).toContain('+1');
  });

  it('should expand roles when clicking the table cell', () => {
    fixture.detectChanges();

    httpTesting
      .expectOne((r) => r.url.endsWith('/user/all/no-roles'))
      .flush({ users: [], nextPageToken: null });
    httpTesting
      .expectOne((r) => r.url.endsWith('/user/all'))
      .flush({ users: mockUsers, nextPageToken: null });

    fixture.detectChanges();

    // Zoek de klikbare cel voor de 2e gebruiker
    const clickableCell = fixture.debugElement.queryAll(By.css('td.cursor-pointer'))[1];
    expect(clickableCell.nativeElement.textContent).toContain('+1'); // Voor het klikken

    // Simuleer een klik
    clickableCell.triggerEventHandler('click', new Event('click'));
    fixture.detectChanges();

    // Nu zou het +1 label weg moeten zijn, en de 'Minder' ('less') knop moeten verschijnen
    expect(clickableCell.nativeElement.textContent).not.toContain('+1');
    expect(component.expandedRoles().has('many@cloudmen.com')).toBe(true);
  });

  it('should reload data when language changes', () => {
    fixture.detectChanges();

    // Eerste lading afhandelen
    httpTesting.expectOne((r) => r.url.endsWith('/user/all/no-roles')).flush({ users: [] });
    httpTesting.expectOne((r) => r.url.endsWith('/user/all')).flush({ users: [] });

    // Verander taal
    translocoService.setActiveLang('nl');

    // Omdat de component subscribet op langChanges$, moeten er nieuwe API calls komen
    const reqsNoRoles = httpTesting.match((r) => r.url.endsWith('/user/all/no-roles'));
    const reqsAll = httpTesting.match((r) => r.url.endsWith('/user/all'));

    expect(reqsNoRoles.length).toBe(1);
    expect(reqsAll.length).toBe(1);
  });
});
