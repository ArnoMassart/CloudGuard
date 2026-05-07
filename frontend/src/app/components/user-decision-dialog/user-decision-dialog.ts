import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-user-decision-dialog',
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    LucideAngularModule,
    TranslocoPipe,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './user-decision-dialog.html',
  styleUrl: './user-decision-dialog.css',
})
export class UserDecisionDialog {
  readonly Icons = AppIcons;
  readonly maxStepAccept = 4;

  step: number = 1;

  selectedOrganizationId: string = '';
  isSuperAdmin: boolean = false;
  selectedRoles: Set<string> = new Set();
  denyReason: string = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dialogRef: MatDialogRef<UserDecisionDialog>
  ) {}

  selectedOrgName(): string {
    const id = this.selectedOrganizationId;
    if (!id || !this.data.uniqueOrganizations?.length) {
      return '\u2014';
    }
    const org = this.data.uniqueOrganizations.find(
      (o: { id: string | number; name: string }) => String(o.id) === String(id),
    );
    return org?.name ?? '\u2014';
  }

  /** Role label keys for Transloco (`regularRoles` uses `label`, not `labelKey`). */
  summaryRoles(): { labelKey: string; trackId: string }[] {
    if (this.isSuperAdmin) {
      return [{ labelKey: 'user.role.super-admin', trackId: '__super_admin__' }];
    }

    const roles = this.data.regularRoles ?? [];
    return Array.from(this.selectedRoles).map((value) => {
      const r = roles.find((x: { value: string; label: string }) => x.value === value);
      return { labelKey: r?.label ?? value, trackId: value };
    });
  }

  canAdvanceFromRolesStep(): boolean {
    return this.isSuperAdmin || this.selectedRoles.size > 0;
  }

  onOrgFilterChange(orgId: string) {
    this.selectedOrganizationId = orgId;
  }

  toggleRole(roleValue: string, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.selectedRoles.add(roleValue);
    } else {
      this.selectedRoles.delete(roleValue);
    }
  }

  nextStep() {
    if (!this.data.isAccepted) {
      this.submit();
      return;
    }
    if (this.step < this.maxStepAccept) {
      this.step++;
    }
  }

  confirmAndSubmit(){
    this.submit();
  }

  prevStep() {
    if (this.step > 1) {
      this.step--;
    }
  }

  submit() {
    // We bundelen ALLES en sturen het terug naar het component dat de dialog opende
    this.dialogRef.close({
      userEmail: this.data.user.email,
      isAccepted: this.data.isAccepted,
      organizationId: this.selectedOrganizationId,
      isSuperAdmin: this.isSuperAdmin,
      roles: Array.from(this.selectedRoles),
      denyReason: this.denyReason.trim(),
    });
  }
}
