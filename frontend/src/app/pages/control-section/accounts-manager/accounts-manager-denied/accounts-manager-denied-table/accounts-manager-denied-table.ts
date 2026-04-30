import { Component, inject, input, output } from '@angular/core';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Organization } from '../../../../models/org/Organization';
import { AppIcons } from '../../../../shared/AppIcons';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-accounts-manager-denied-table',
  imports: [TranslocoPipe, FormsModule, CommonModule, LucideAngularModule],
  templateUrl: './accounts-manager-denied-table.html',
  styleUrl: './accounts-manager-denied-table.css',
})
export class AccountsManagerDeniedTable {
  readonly Icons = AppIcons;

  readonly #translocoService = inject(TranslocoService);

  // Inputs
  readonly users = input.required<User[]>();
  readonly orgs = input.required<Organization[]>();
  readonly hasExistingRoles = input.required<boolean>();
  readonly expandedRoles = input.required<Set<string>>();

  // Outputs
  readonly actionClick = output<User>();
  readonly orgChange = output<{ user: User; newOrgId: number | null }>();
  readonly toggleRoles = output<{ email: string; length: number }>();

  /** Chips shown in the Role column (Super Admin + Cloudmen admin are two chips when both apply). */
  getRolesTranslated(user: User): {
    trackKey: string;
    value: Role;
    label: string;
    isCloudmenAdminLabel: boolean;
  }[] {
    if (!user.roles) return [];
    const chips = user.roles.flatMap((role) => {
      const typedRole = role as Role;
      if (typedRole === Role.SUPER_ADMIN && this.isRowCloudmenStaffUser(user)) {
        return [
          {
            trackKey: 'SUPER_ADMIN',
            value: Role.SUPER_ADMIN,
            label: RoleLabels[Role.SUPER_ADMIN],
            isCloudmenAdminLabel: false,
          },
          {
            trackKey: 'CLOUDMEN_ADMIN',
            value: Role.SUPER_ADMIN,
            label: 'account-manager.cloudmen-staff-badge',
            isCloudmenAdminLabel: true,
          },
        ];
      }
      return [
        {
          trackKey: typedRole,
          value: typedRole,
          label: RoleLabels[typedRole],
          isCloudmenAdminLabel: false,
        },
      ];
    });
    return chips.sort((a, b) => {
      const priorityA = RolePriority[a.value] ?? 99;
      const priorityB = RolePriority[b.value] ?? 99;
      if (priorityA !== priorityB) {
        return priorityA - priorityB;
      }
      if (a.isCloudmenAdminLabel !== b.isCloudmenAdminLabel) {
        return a.isCloudmenAdminLabel ? -1 : 1;
      }
      const translatedA = this.#translocoService.translate(a.label);
      const translatedB = this.#translocoService.translate(b.label);
      return translatedA.localeCompare(translatedB);
    });
  }

  displayRoleChipCount(user: User): number {
    return this.getRolesTranslated(user).length;
  }

  onOrgChange(user: User, event: Event) {
    this.orgChange.emit({ user, newOrgId: user.organizationId });
  }

  isRoleChange(): boolean {
    return this.users().some((user) => user.roleRequested);
  }

  isOrgChange(): boolean {
    return this.users().some((user) => user.organizationRequested);
  }

  getRoleColorClasses(roleValue: Role, isCloudmenAdminLabel: boolean): string {
    if (isCloudmenAdminLabel) {
      return 'bg-primary text-white border-primary';
    }
    switch (roleValue) {
      case Role.SUPER_ADMIN:
        return 'bg-blue-100 text-blue-700 border-blue-200';
      case Role.UNASSIGNED:
        return 'bg-gray-100 text-gray-500 border-gray-200';
      default:
        return 'bg-emerald-50 text-emerald-600 border-emerald-200';
    }
  }

  /** Row user is on the server Cloudmen staff allowlist (from API UserDto). */
  isRowCloudmenStaffUser(user: { isCloudmenStaff?: boolean }): boolean {
    return user.isCloudmenStaff === true;
  }
}
