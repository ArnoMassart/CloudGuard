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

  step: number = 1;

  selectedOrganizationId: string = '';
  isSuperAdmin: boolean = false;
  selectedRoles: Set<string> = new Set();
  denyReason: string = '';

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dialogRef: MatDialogRef<UserDecisionDialog>
  ) {}

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
    // Als het een weigering is, zijn we direct klaar
    if (!this.data.isAccepted) {
      this.submit();
      return;
    }

    // Anders gaan we naar de volgende stap
    if (this.step < 3) {
      this.step++;
    } else {
      this.submit();
    }
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
