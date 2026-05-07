import { CommonModule } from '@angular/common';
import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { Subscription, distinctUntilChanged } from 'rxjs';
import { AssignRoleDialog } from '../../../../components/assign-role-dialog/assign-role-dialog';
import { PaginationBar } from '../../../../components/pagination-bar/pagination-bar';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { Organization } from '../../../../models/org/Organization';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { OrgService } from '../../../../services/org-service';
import { UserService } from '../../../../services/user-service';
import { AppIcons } from '../../../../shared/AppIcons';
import { AccountsManagerTable } from '../accounts-manager-table/accounts-manager-table';
import { DeactivateUserDialog } from '../../../../components/deactivate-user-dialog/deactivate-user-dialog';
import { AccountStatus } from '../../../../models/account/AccountStatus';

const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-accounts-manager-users',
  imports: [
    TranslocoPipe,
    SearchBar,
    PaginationBar,
    LucideAngularModule,
    FormsModule,
    AccountsManagerTable,
    CommonModule,
  ],
  templateUrl: './accounts-manager-users.html',
  styleUrl: './accounts-manager-users.css',
})
export class AccountsManagerUsers {
  readonly Icons = AppIcons;
  readonly AccountStatus = AccountStatus;
  readonly userService = inject(UserService);
  readonly #translocoService = inject(TranslocoService);
  readonly #orgService = inject(OrgService);

  readonly pagination = viewChild(PaginationBar);

  readonly users = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly searchQuery = signal('');

  readonly nextPageToken = signal<string | null>(null);
  readonly dialog = inject(MatDialog);

  readonly expandedRoles = signal<Set<string>>(new Set<string>());
  readonly statusOptions = Object.values(AccountStatus);

  toggleRolesSummary(email: string, rolesLength: number) {
    if (rolesLength > 2) {
      const current = new Set(this.expandedRoles());
      if (current.has(email)) {
        current.delete(email);
      } else {
        current.add(email);
      }
      this.expandedRoles.set(current);
    }
  }

  readonly orgs = signal<Organization[]>([]);

  readonly selectedOrganization = signal<string>('');
  readonly uniqueOrganizations = signal<{ id: string; name: string }[]>([
    { id: '', name: 'account-manager.all-orgs' },
  ]); // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  private langSubscription?: Subscription;

  readonly selectedStatus = signal<AccountStatus>(AccountStatus.All);

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    const reload = () => {
      this.loadUsers();
      this.loadOrganizations();
    };
    reload();
    this.langSubscription = this.#translocoService.langChanges$
      .pipe(distinctUntilChanged())
      .subscribe(reload);
  }

  ngOnDestroy(): void {
    if (this.langSubscription) {
      this.langSubscription.unsubscribe();
    }
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  onOrgFilterChange(orgId: string) {
    this.selectedOrganization.set(orgId);
    this.#resetAndLoad();
  }

  onStatusChange(newStatus: AccountStatus) {
    this.selectedStatus.set(newStatus);
    this.#resetAndLoad();
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.pagination()?.reset();
    this.loadUsers();
  }

  getAllAvailableRoles() {
    return Object.values(Role)
      .filter((role) => (role as Role) !== Role.UNASSIGNED)
      .map((role) => {
        const typedRole = role as Role;
        return {
          value: typedRole,
          label: RoleLabels[typedRole],
        };
      })
      .sort((a, b) => {
        const translatedA = this.#translocoService.translate(a.label);
        const translatedB = this.#translocoService.translate(b.label);

        return translatedA.localeCompare(translatedB);
      });
  }

  getRolesTranslated(user: { roles: Role[] }) {
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

  openRoleChangeDialog(user: User, hasExistingRoles: boolean): void {
    const dialogRef = this.dialog.open(AssignRoleDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isEditMode: hasExistingRoles,
        allAvailableRoles: this.getAllAvailableRoles(),
      },
    });

    dialogRef.afterClosed().subscribe((newRoles) => {
      if (newRoles) {
        this.updateRoles(user.email, newRoles);
      }
    });
  }

  updateRoles(email: string, roles: Role[]) {
    this.isLoading.set(true);
    this.userService.updateRolesForUser(email, roles).subscribe({
      next: () => {
        this.loadUsers();
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error occured with updating roles', err);
        this.isLoading.set(false);
      },
    });
  }

  loadUsers(token?: string) {
    this.isLoading.set(true);

    this.userService
      .getAllDatabaseUsers(
        ITEMS_PER_PAGE,
        token || undefined,
        this.searchQuery(),
        this.selectedOrganization(),
        this.selectedStatus()
      )
      .subscribe({
        next: (page) => {
          this.users.set(page.users);
          this.nextPageToken.set(page.nextPageToken);
          this.isLoading.set(false);
          this.userService.refreshDeniedCount();
        },
        error: (err) => {
          console.error('Failed to load users', err);
          this.isLoading.set(false);
        },
      });
  }

  loadOrganizations() {
    this.#orgService.getAllOrgs().subscribe({
      next: (orgs) => {
        this.orgs.set(orgs);

        const mappedOrgs = orgs.map((o) => ({
          id: o.id.toString(),
          name: o.name,
        }));

        // Dit overschrijft de startwaarde met de volledige lijst
        this.uniqueOrganizations.set([{ id: '', name: 'account-manager.all-orgs' }, ...mappedOrgs]);
      },
      error: (err) => console.error('Fout bij laden organisaties', err),
    });
  }

  onOrganizationChange(user: User) {
    const newOrgId = user.organizationId;

    // Roep je UserService aan om de organisatie van de gebruiker te updaten
    this.userService.updateUserOrg(user.email, newOrgId).subscribe({
      next: () => {
        // Eventueel een succesmelding of de lokale state updaten
        this.loadUsers();
      },
    });
  }

  isOrgInList(orgId: number | null): boolean {
    if (!orgId) return false;
    return this.orgs().some((org) => org.id === orgId);
  }

  #resetAndLoad() {
    this.pagination()?.reset();
    this.loadUsers();
  }

  hasRequest(): boolean {
    return this.users().some((u) => u.organizationRequested || u.roleRequested);
  }

  switchUserActiveState(user: User) {
    const ref = this.dialog.open(DeactivateUserDialog, {
      width: '400px',
      panelClass: 'dialog-panel',
      backdropClass: 'dialog-backdrop',
      data: {
        user: user,
      },
    });

    ref.afterClosed().subscribe((result) => {
      if (result) {
        this.userService.switchUserStatus(user.email).subscribe({
          next: () => {
            this.#resetAndLoad();
          },
          error: (err) => {
            console.error('Error with setting user to inactive', err);
          },
        });
      }
    });
  }

  getCorrectTranslocoKeyForStatus(status: AccountStatus): string {
    switch (status) {
      case AccountStatus.Active:
        return 'active-2';
      case AccountStatus.Inactive:
        return 'inactive-caps';
      default:
        return 'devices.all-states';
    }
  }
}
