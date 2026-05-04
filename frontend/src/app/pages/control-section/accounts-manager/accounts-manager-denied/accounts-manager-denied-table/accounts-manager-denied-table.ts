import { Component, inject, input, output } from '@angular/core';
import { Role, RoleLabels, RolePriority, User } from '../../../../../models/users/User';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Organization } from '../../../../../models/org/Organization';
import { AppIcons } from '../../../../../shared/AppIcons';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { DeniedUser } from '../../../../../models/users/DeniedUser';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-accounts-manager-denied-table',
  imports: [TranslocoPipe, FormsModule, CommonModule, LucideAngularModule, MatTooltipModule],
  templateUrl: './accounts-manager-denied-table.html',
  styleUrl: './accounts-manager-denied-table.css',
})
export class AccountsManagerDeniedTable {
  readonly Icons = AppIcons;

  // Inputs
  readonly users = input.required<DeniedUser[]>();

  // Outputs
  readonly actionClick = output<DeniedUser>();

  /** Row user is on the server Cloudmen staff allowlist (from API UserDto). */
  isRowCloudmenStaffUser(user: { isCloudmenStaff?: boolean }): boolean {
    return user.isCloudmenStaff === true;
  }
}
