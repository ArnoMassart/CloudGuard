import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeactivateUserDialog } from './deactivate-user-dialog';

describe('DeactivateUserDialog', () => {
  let component: DeactivateUserDialog;
  let fixture: ComponentFixture<DeactivateUserDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeactivateUserDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(DeactivateUserDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
