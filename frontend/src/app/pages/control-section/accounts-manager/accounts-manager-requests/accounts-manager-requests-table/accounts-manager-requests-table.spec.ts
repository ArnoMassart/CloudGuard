import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerRequestsTable } from './accounts-manager-requests-table';

describe('AccountsManagerRequestsTable', () => {
  let component: AccountsManagerRequestsTable;
  let fixture: ComponentFixture<AccountsManagerRequestsTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerRequestsTable],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerRequestsTable);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
