import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { Role, User } from '../../models/users/User';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface RoleOption {
  value: Role;
  label: string;
}

export interface RoleDialogData {
  user: User;
  isEditMode: boolean;
  allAvailableRoles: RoleOption[];
}

@Component({
  selector: 'app-assign-role-dialog',
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    LucideAngularModule,
    TranslocoPipe,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './assign-role-dialog.html',
  styleUrl: './assign-role-dialog.css',
})
export class AssignRoleDialog {
  readonly Icons = AppIcons;

  isSuperAdmin = false;
  regularRoles: RoleOption[] = [];
  selectedRoles = new Set<Role>();

  constructor(
    public dialogRef: MatDialogRef<AssignRoleDialog>,
    @Inject(MAT_DIALOG_DATA) public data: RoleDialogData
  ) {}

  ngOnInit() {
    // 4. Filter op basis van de 'value' eigenschap
    this.regularRoles = this.data.allAvailableRoles.filter(
      (roleObj) => roleObj.value !== Role.SUPER_ADMIN
    );

    if (this.data.user.roles && this.data.user.roles.length > 0) {
      if (this.data.user.roles.includes(Role.SUPER_ADMIN)) {
        this.isSuperAdmin = true;
      }

      this.data.user.roles.forEach((role) => {
        if (role !== Role.SUPER_ADMIN && role !== Role.UNASSIGNED) {
          this.selectedRoles.add(role);
        }
      });
    }
  }

  toggleRole(role: Role, event: Event) {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.selectedRoles.add(role);
    } else {
      this.selectedRoles.delete(role);
    }
  }

  save() {
    const finalRoles: Role[] = [];

    if (this.isSuperAdmin) {
      finalRoles.push(Role.SUPER_ADMIN);
    } else {
      finalRoles.push(...Array.from(this.selectedRoles));
    }

    this.dialogRef.close(finalRoles);
  }
}
