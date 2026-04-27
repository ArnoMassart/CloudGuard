import { CommonModule } from '@angular/common';
import { Component, inject, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Organization } from '../../../../models/org/Organization';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../../../shared/AppIcons';

@Component({
  selector: 'app-accounts-manager-table',
  imports: [TranslocoPipe, FormsModule, CommonModule, LucideAngularModule],
  templateUrl: './accounts-manager-table.html',
  styleUrl: './accounts-manager-table.css',
})
export class AccountsManagerTable {
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

  getRolesTranslated(user: { roles: Role[] }) {
    if (!user.roles) return [];
    return user.roles
      .map((role) => {
        const typedRole = role as Role;
        return {
          value: typedRole,
          label: RoleLabels[typedRole],
        };
      })
      .sort((a, b) => {
        const priorityA = RolePriority[a.value] ?? 99;
        const priorityB = RolePriority[b.value] ?? 99;

        if (priorityA === priorityB) {
          const translatedA = this.#translocoService.translate(a.label);
          const translatedB = this.#translocoService.translate(b.label);
          return translatedA.localeCompare(translatedB);
        }
        return priorityA - priorityB;
      });
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

  getRoleColorClasses(roleValue: Role, user: { isCloudmenStaff?: boolean }): string {
    if (this.isRowCloudmenStaffUser(user) && roleValue === Role.SUPER_ADMIN) {
      return 'bg-primary text-white border-primary';
    }
    switch (roleValue) {
      case Role.SUPER_ADMIN:
        return 'bg-purple-100 text-purple-700 border-purple-200';
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
