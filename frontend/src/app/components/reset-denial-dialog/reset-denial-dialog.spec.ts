import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { ResetDenialDialog } from './reset-denial-dialog';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('ResetDenialDialog', () => {
  let component: ResetDenialDialog;
  let fixture: ComponentFixture<ResetDenialDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetDenialDialog],
      providers: [
        provideTranslocoTesting(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { user: { email: 'denied@example.com' } } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetDenialDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
