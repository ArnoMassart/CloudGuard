import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerTable } from './accounts-manager-table';
import { provideTranslocoTesting } from '../../../../testing/transloco-testing';

describe('AccountsManagerTable', () => {
  let component: AccountsManagerTable;
  let fixture: ComponentFixture<AccountsManagerTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerTable],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerTable);
    fixture.componentRef.setInput('users', []);
    fixture.componentRef.setInput('orgs', []);
    fixture.componentRef.setInput('hasRequest', true);
    fixture.componentRef.setInput('expandedRoles', new Set<string>());
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
