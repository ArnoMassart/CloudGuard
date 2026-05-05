import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { UserDecisionDialog } from './user-decision-dialog';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('UserDecisionDialog', () => {
  let component: UserDecisionDialog;
  let fixture: ComponentFixture<UserDecisionDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserDecisionDialog],
      providers: [
        provideTranslocoTesting(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            isAccepted: true,
            user: { firstName: 'U', lastName: 'User', email: 'req@example.com' },
            organizations: [],
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UserDecisionDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
