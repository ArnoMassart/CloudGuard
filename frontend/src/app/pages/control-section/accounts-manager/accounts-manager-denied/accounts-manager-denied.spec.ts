import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerDenied } from './accounts-manager-denied';
import { provideTranslocoTesting } from '../../../../testing/transloco-testing';

describe('AccountsManagerDenied', () => {
  let component: AccountsManagerDenied;
  let fixture: ComponentFixture<AccountsManagerDenied>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerDenied],
      providers: [provideTranslocoTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerDenied);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
