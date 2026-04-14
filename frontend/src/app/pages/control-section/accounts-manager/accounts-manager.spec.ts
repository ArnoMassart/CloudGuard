import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManager } from './accounts-manager';

describe('AccountsManager', () => {
  let component: AccountsManager;
  let fixture: ComponentFixture<AccountsManager>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManager],
    }).compileComponents();

    fixture = TestBed.createComponent(AccountsManager);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
