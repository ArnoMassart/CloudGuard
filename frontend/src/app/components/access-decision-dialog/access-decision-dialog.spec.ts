import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { AccessDecisionDialog } from './access-decision-dialog';
import { provideTranslocoTesting } from '../../testing/transloco-testing';
import { UserService } from '../../services/user-service';
import { User } from '../../models/users/User';

describe('AccessDecisionDialog', () => {
  let component: AccessDecisionDialog;
  let fixture: ComponentFixture<AccessDecisionDialog>;

  const dialogUser: User = (() => {
    const now = new Date();
    return {
      email: 'u@example.com',
      firstName: 'U',
      lastName: 'User',
      roles: [],
      createdAt: now,
      isActive: true,
      roleRequested: false,
      organizationRequested: false,
      organizationId: 1,
      accessRequested: false,
      accessAccepted: false,
      accessDenied: false,
      accessDeniedAt: now,
      accessDeniedReason: '',
      accessRequestedAt: now,
    };
  })();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AccessDecisionDialog],
      providers: [
        provideTranslocoTesting(),
        { provide: MatDialogRef, useValue: { close: vi.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { user: dialogUser, isAccepted: true },
        },
        {
          provide: UserService,
          useValue: {
            getUsersFullName: (u: User) => `${u.firstName} ${u.lastName}`,
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccessDecisionDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
