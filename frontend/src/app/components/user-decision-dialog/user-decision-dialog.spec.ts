import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

import { UserDecisionDialog } from './user-decision-dialog';
import { provideTranslocoTesting } from '../../testing/transloco-testing';

describe('UserDecisionDialog', () => {
  let component: UserDecisionDialog;
  let fixture: ComponentFixture<UserDecisionDialog>;
  let dialogClose: ReturnType<typeof vi.fn>;

  const baseData = () => ({
    isAccepted: true,
    user: { firstName: 'U', lastName: 'User', email: 'req@example.com' },
    organizations: [],
    uniqueOrganizations: [
      { id: 1, name: 'Org One' },
      { id: 2, name: 'Org Two' },
    ],
    regularRoles: [
      { value: 'VIEWER', label: 'Viewer Role' },
      { value: 'EDITOR', label: 'Editor Role' },
    ],
  });

  beforeEach(async () => {
    dialogClose = vi.fn();
    await TestBed.configureTestingModule({
      imports: [UserDecisionDialog],
      providers: [
        provideTranslocoTesting(),
        { provide: MatDialogRef, useValue: { close: dialogClose } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: baseData(),
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

  it('selectedOrgName returns en-dash when nothing selected', () => {
    component.selectedOrganizationId = '';
    expect(component.selectedOrgName()).toBe('\u2014');
  });

  it('selectedOrgName returns matching organization name', () => {
    component.selectedOrganizationId = '2';
    expect(component.selectedOrgName()).toBe('Org Two');
  });

  it('summaryRoles returns super-admin entry when isSuperAdmin', () => {
    component.isSuperAdmin = true;
    expect(component.summaryRoles()).toEqual([
      { labelKey: 'user.role.super-admin', trackId: '__super_admin__' },
    ]);
  });

  it('summaryRoles maps selected role values to labels from regularRoles', () => {
    component.isSuperAdmin = false;
    component.selectedRoles = new Set(['VIEWER']);
    expect(component.summaryRoles()).toEqual([{ labelKey: 'Viewer Role', trackId: 'VIEWER' }]);
  });

  it('summaryRoles falls back to raw value when role metadata missing', () => {
    component.isSuperAdmin = false;
    component.selectedRoles = new Set(['UNKNOWN']);
    expect(component.summaryRoles()).toEqual([{ labelKey: 'UNKNOWN', trackId: 'UNKNOWN' }]);
  });

  it('canAdvanceFromRolesStep requires at least one role unless super admin', () => {
    component.isSuperAdmin = false;
    component.selectedRoles = new Set();
    expect(component.canAdvanceFromRolesStep()).toBe(false);
    component.selectedRoles.add('VIEWER');
    expect(component.canAdvanceFromRolesStep()).toBe(true);
    component.isSuperAdmin = true;
    component.selectedRoles = new Set();
    expect(component.canAdvanceFromRolesStep()).toBe(true);
  });

  it('nextStep submits immediately when dialog is deny flow', () => {
    component.data.isAccepted = false;
    component.nextStep();
    expect(dialogClose).toHaveBeenCalled();
  });

  it('confirmAndSubmit closes dialog with payload', () => {
    component.data.isAccepted = true;
    component.step = 4;
    component.selectedOrganizationId = '1';
    component.selectedRoles.add('VIEWER');
    component.confirmAndSubmit();
    expect(dialogClose).toHaveBeenCalledWith(
      expect.objectContaining({
        userEmail: 'req@example.com',
        isAccepted: true,
        organizationId: '1',
        roles: ['VIEWER'],
      }),
    );
  });
});
