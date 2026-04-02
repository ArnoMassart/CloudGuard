import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { of } from 'rxjs';
import { UsersGroups } from './users-groups';
import { SectionType } from '../../../models/SectionType';

// Mock vertalingen
const I18N_MOCK = {
  users: 'Gebruikers',
  groups: 'Groepen',
};

class UsersGroupsTranslocoLoader implements TranslocoLoader {
  getTranslation() {
    return of(I18N_MOCK);
  }
}

describe('UsersGroups', () => {
  let component: UsersGroups;
  let fixture: ComponentFixture<UsersGroups>;

  beforeEach(async () => {
    // Reset sessionStorage voor elke test
    sessionStorage.clear();

    await TestBed.configureTestingModule({
      imports: [UsersGroups],
      providers: [
        provideTransloco({
          config: {
            availableLangs: ['en', 'nl'],
            defaultLang: 'en',
          },
          loader: UsersGroupsTranslocoLoader,
        }),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersGroups);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  describe('ngOnInit (Initialization)', () => {
    it('should default to USERS if sessionStorage is empty', () => {
      fixture.detectChanges();
      expect(component.currentSection()).toBe('USERS');
    });

    it('should load GROUPS from sessionStorage if present', () => {
      sessionStorage.setItem('user-group-section', 'GROUPS');
      fixture.detectChanges();
      expect(component.currentSection()).toBe('GROUPS');
    });

    it('should fallback to USERS if sessionStorage contains invalid value', () => {
      sessionStorage.setItem('user-group-section', 'INVALID');
      fixture.detectChanges();
      expect(component.currentSection()).toBe('USERS');
    });
  });

  describe('togglePage', () => {
    it('should update currentSection and sessionStorage when a different section is selected', () => {
      fixture.detectChanges();
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');

      component.togglePage('GROUPS');

      expect(component.currentSection()).toBe('GROUPS');
      expect(setItemSpy).toHaveBeenCalledWith('user-group-section', 'GROUPS');
    });

    it('should not update or setItem if the section is already active', () => {
      fixture.detectChanges(); // Staat op USERS
      const setItemSpy = vi.spyOn(Storage.prototype, 'setItem');

      component.togglePage('USERS');

      expect(setItemSpy).not.toHaveBeenCalled();
    });
  });

  describe('getTabClass', () => {
    it('should return active classes when section is active', () => {
      fixture.detectChanges();
      // Default is USERS
      const classes = component.getTabClass('USERS');

      expect(classes['border-primary text-black']).toBe(true);
      expect(classes['border-transparent text-slate-500 hover:text-slate-700 cursor-pointer']).toBe(
        false
      );
    });

    it('should return inactive classes when section is not active', () => {
      fixture.detectChanges();
      // Default is USERS, dus GROUPS is inactive
      const classes = component.getTabClass('GROUPS');

      expect(classes['border-primary text-black']).toBe(false);
      expect(classes['border-transparent text-slate-500 hover:text-slate-700 cursor-pointer']).toBe(
        true
      );
    });
  });
});
