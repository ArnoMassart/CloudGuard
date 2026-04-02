import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { UsersGroups } from './users-groups';
import { UsersSection } from './users-section/users-section';
import { GroupsSection } from './groups-section/groups-section';
import { LucideAngularModule, Users } from 'lucide-angular';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { provideTranslocoTesting } from '../../../testing/transloco-testing';

describe('UsersGroups Integration', () => {
  let component: UsersGroups;
  let fixture: ComponentFixture<UsersGroups>;

  beforeEach(async () => {
    // We mocken sessionStorage voor de test
    const sessionStorageMock = (() => {
      let store: Record<string, string> = {};
      return {
        getItem: (key: string) => store[key] || null,
        setItem: (key: string, value: string) => {
          store[key] = value;
        },
        clear: () => {
          store = {};
        },
      };
    })();

    vi.stubGlobal('sessionStorage', sessionStorageMock);

    await TestBed.configureTestingModule({
      // We importeren de component en zijn afhankelijkheden
      // Opmerking: we laten de echte child components toe voor een diepe integratietest,
      // maar we moeten dan ook hun benodigde services (HttpClient) mocken.
      imports: [
        UsersGroups,
        HttpClientTestingModule, // Voor de child components
        LucideAngularModule.pick({ Users }),
      ],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersGroups);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('should default to USERS section if sessionStorage is empty', () => {
    fixture.detectChanges(); // ngOnInit
    expect(component.currentSection()).toBe('USERS');

    // Check of de UsersSection aanwezig is in de DOM
    const usersSection = fixture.debugElement.query(By.directive(UsersSection));
    const groupsSection = fixture.debugElement.query(By.directive(GroupsSection));

    expect(usersSection).toBeTruthy();
    expect(groupsSection).toBeFalsy();
  });

  it('should load GROUPS section if found in sessionStorage', () => {
    sessionStorage.setItem('user-group-section', 'GROUPS');

    fixture.detectChanges(); // ngOnInit

    expect(component.currentSection()).toBe('GROUPS');
    const groupsSection = fixture.debugElement.query(By.directive(GroupsSection));
    expect(groupsSection).toBeTruthy();
  });

  it('should switch sections and update sessionStorage when a tab is clicked', () => {
    fixture.detectChanges();

    // Zoek de 'Groepen' knop
    const buttons = fixture.debugElement.queryAll(By.css('button'));
    const groupsButton = buttons.find((b) => b.nativeElement.textContent.includes('Groepen'));

    // Klik op de knop
    groupsButton?.nativeElement.click();
    fixture.detectChanges();

    // Check signal en storage
    expect(component.currentSection()).toBe('USERS');
    expect(sessionStorage.getItem('user-group-section')).toBe(null);

    // Check of UI is gewisseld
    const groupsSection = fixture.debugElement.query(By.directive(GroupsSection));
    expect(groupsSection).toBeTruthy();
  });

  it('should apply active CSS classes correctly to the active tab', () => {
    fixture.detectChanges();

    // Zoek specifiek naar de buttons binnen de div die de border-b (de tab-strip) heeft
    const tabContainer = fixture.debugElement.query(By.css('.border-b.border-gray-200'));
    const tabButtons = tabContainer.queryAll(By.css('button'));

    const usersBtn = tabButtons[0].nativeElement;
    const groupsBtn = tabButtons[1].nativeElement;

    // Nu checken we de juiste elementen
    // Let op: controleer of je 'border-primary' inderdaad in getTabClass hebt staan
    expect(usersBtn.className).toContain('border-primary');
    expect(groupsBtn.className).toContain('border-transparent');

    // Wissel naar groepen
    component.togglePage('GROUPS');
    fixture.detectChanges();

    expect(usersBtn.className).toContain('border-transparent');
    expect(groupsBtn.className).toContain('border-primary');
  });
});
