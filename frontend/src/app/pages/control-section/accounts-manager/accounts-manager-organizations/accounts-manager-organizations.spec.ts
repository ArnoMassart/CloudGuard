import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerOrganizations } from './accounts-manager-organizations';
import { provideTranslocoTesting } from '../../../../testing/transloco-testing';

describe('AccountsManagerOrganizations', () => {
  let component: AccountsManagerOrganizations;
  let fixture: ComponentFixture<AccountsManagerOrganizations>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerOrganizations],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerOrganizations);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
