import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccountsManagerTable } from './accounts-manager-table';

describe('AccountsManagerTable', () => {
  let component: AccountsManagerTable;
  let fixture: ComponentFixture<AccountsManagerTable>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccountsManagerTable]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AccountsManagerTable);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
