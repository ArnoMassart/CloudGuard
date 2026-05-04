import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AccessDecisionDialog } from './access-decision-dialog';

describe('AccessDecisionDialog', () => {
  let component: AccessDecisionDialog;
  let fixture: ComponentFixture<AccessDecisionDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccessDecisionDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(AccessDecisionDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
