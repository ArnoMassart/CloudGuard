import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerUsers } from './accounts-manager-users';

describe('AccountsManagerUsers', () => {
  let component: AccountsManagerUsers;
  let fixture: ComponentFixture<AccountsManagerUsers>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerUsers],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerUsers);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
