import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserDecisionDialog } from './user-decision-dialog';

describe('UserDecisionDialog', () => {
  let component: UserDecisionDialog;
  let fixture: ComponentFixture<UserDecisionDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDecisionDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDecisionDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
