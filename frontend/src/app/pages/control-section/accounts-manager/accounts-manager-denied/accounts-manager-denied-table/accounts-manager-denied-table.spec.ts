import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerDeniedTable } from './accounts-manager-denied-table';
import { provideTranslocoTesting } from '../../../../../testing/transloco-testing';

describe('AccountsManagerDeniedTable', () => {
  let component: AccountsManagerDeniedTable;
  let fixture: ComponentFixture<AccountsManagerDeniedTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerDeniedTable],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerDeniedTable);
    fixture.componentRef.setInput('users', []);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
