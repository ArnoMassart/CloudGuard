import { Component, effect, inject, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { MatDialog } from '@angular/material/dialog';
import { ViewRolesDialog } from '../../components/view-roles-dialog/view-roles-dialog';
import { Role, RoleLabels, User } from '../../models/users/User';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, LucideAngularModule, TranslocoPipe],
  templateUrl: './profile.html',
})
export class Profile {
  readonly Icons = AppIcons;
  readonly authService = inject(CustomAuthService);
  readonly currentUser = this.authService.currentUser;
  readonly userService = inject(UserService);
  readonly translocoService = inject(TranslocoService);
  readonly profileImageError = signal(false);
  closed$ = output<void>();

  readonly dialog = inject(MatDialog);

  constructor() {
    effect(() => {
      this.currentUser();
      this.profileImageError.set(false);
    });
  }

  close() {
    this.closed$.emit();
  }

  logout() {
    this.authService.logout();
  }

  getSortedTranslatedRoles(user: User) {
    return user.roles
      .map((role) => ({
        value: role,
        label: RoleLabels[role],
      }))
      .sort((a, b) => {
        const transA = this.translocoService.translate(a.label);
        const transB = this.translocoService.translate(b.label);
        return transA.localeCompare(transB);
      });
  }

  getRoleName(user: { roles: Role[] }): string {
    const role = user.roles.at(0);

    if (role === Role.SUPER_ADMIN) {
      return RoleLabels[Role.SUPER_ADMIN];
    }

    return 'viewer';
  }

  openRolesDialog(user: User) {
    if (user.roles.length <= 1) return;

    this.dialog.open(ViewRolesDialog, {
      width: '400px',
      panelClass: 'custom-dialog-container',
      data: {
        roles: this.getSortedTranslatedRoles(user),
      },
    });
  }
}
