import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerRequests } from './accounts-manager-requests';

describe('AccountsManagerRequests', () => {
  let component: AccountsManagerRequests;
  let fixture: ComponentFixture<AccountsManagerRequests>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerRequests],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManagerRequests);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
