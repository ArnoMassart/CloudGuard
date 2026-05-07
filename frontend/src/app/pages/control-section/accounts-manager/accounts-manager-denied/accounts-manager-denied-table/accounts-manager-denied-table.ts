import { Component, input, output } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { UserAvatar } from '../../../../../components/user-avatar/user-avatar';
import { AppIcons } from '../../../../../shared/AppIcons';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { DeniedUser } from '../../../../../models/users/DeniedUser';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-accounts-manager-denied-table',
  imports: [TranslocoPipe, FormsModule, CommonModule, LucideAngularModule, MatTooltipModule, UserAvatar],
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

  deniedUserInitials(user: DeniedUser): string {
    const parts = user.name.trim().split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    if (parts.length === 1 && parts[0].length >= 2) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    if (parts.length === 1) {
      return (parts[0][0] + (parts[0][1] ?? '?')).toUpperCase();
    }
    return user.email.slice(0, 2).toUpperCase();
  }
}
