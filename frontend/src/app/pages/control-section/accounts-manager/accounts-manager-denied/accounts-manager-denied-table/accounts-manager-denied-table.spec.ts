import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerDeniedTable } from './accounts-manager-denied-table';

describe('AccountsManagerDeniedTable', () => {
  let component: AccountsManagerDeniedTable;
  let fixture: ComponentFixture<AccountsManagerDeniedTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerDeniedTable],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerDeniedTable);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
