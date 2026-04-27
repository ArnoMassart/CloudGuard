import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { AccountsManager } from './accounts-manager';

class AccountsManagerTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of({
      'accounts-manager': 'Account Manager',
      users: 'Users',
    });
  }
}

describe('AccountsManager', () => {
  let component: AccountsManager;
  let fixture: ComponentFixture<AccountsManager>;

  beforeEach(async () => {
    sessionStorage.clear();
    await TestBed.configureTestingModule({
      imports: [AccountsManager],
      providers: [
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
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('defaults currentSection to USERS when sessionStorage is empty', () => {
    fixture.detectChanges();
    expect(component.currentSection()).toBe('USERS');
  });

  it('restores section from sessionStorage on init', () => {
    sessionStorage.setItem('account-section', 'ORGANIZATIONS');
    component.ngOnInit();
    expect(component.currentSection()).toBe('ORGANIZATIONS');
  });

  it('togglePage updates currentSection and sessionStorage', () => {
    fixture.detectChanges();
    component.togglePage('ORGANIZATIONS');
    expect(component.currentSection()).toBe('ORGANIZATIONS');
    expect(sessionStorage.getItem('account-section')).toBe('ORGANIZATIONS');
  });

  it('getTabClass marks the active section', () => {
    fixture.detectChanges();
    const usersTab = component.getTabClass('USERS');
    expect(usersTab['border-primary text-black']).toBe(true);
  });
});
