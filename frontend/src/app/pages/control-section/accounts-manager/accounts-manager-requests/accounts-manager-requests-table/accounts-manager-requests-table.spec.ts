import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerRequestsTable } from './accounts-manager-requests-table';
import { provideTranslocoTesting } from '../../../../../testing/transloco-testing';

describe('AccountsManagerRequestsTable', () => {
  let component: AccountsManagerRequestsTable;
  let fixture: ComponentFixture<AccountsManagerRequestsTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerRequestsTable],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerRequestsTable);
    fixture.componentRef.setInput('users', []);
    fixture.componentRef.setInput('orgs', []);
    fixture.componentRef.setInput('hasExistingRoles', true);
    fixture.componentRef.setInput('expandedRoles', new Set<string>());
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
