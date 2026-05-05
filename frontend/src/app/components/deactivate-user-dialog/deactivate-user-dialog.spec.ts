import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { DeactivateUserDialog } from './deactivate-user-dialog';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('DeactivateUserDialog', () => {
  let component: DeactivateUserDialog;
  let fixture: ComponentFixture<DeactivateUserDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeactivateUserDialog],
      providers: [
        provideTranslocoTesting(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { user: { email: 'a@b.com', firstName: 'A', lastName: 'B', isActive: true } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DeactivateUserDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
