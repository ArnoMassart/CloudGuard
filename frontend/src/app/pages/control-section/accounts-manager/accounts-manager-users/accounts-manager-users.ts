import { CommonModule } from '@angular/common';
import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { Subscription } from 'rxjs';
import { AssignRoleDialog } from '../../../../components/assign-role-dialog/assign-role-dialog';
import { PageHeader } from '../../../../components/page-header/page-header';
import { PaginationBar } from '../../../../components/pagination-bar/pagination-bar';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { AccountSectionType } from '../../../../models/AccountSectionType';
import { Organization } from '../../../../models/org/Organization';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { OrgService } from '../../../../services/org-service';
import { UserService } from '../../../../services/user-service';
import { AppIcons } from '../../../../shared/AppIcons';
import { AccountsManagerTable } from '../accounts-manager-table/accounts-manager-table';

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
  readonly #userService = inject(UserService);
  readonly #translocoService = inject(TranslocoService);
  readonly #orgService = inject(OrgService);

  readonly pagination = viewChild(PaginationBar);
  readonly paginationWithoutRoles = viewChild(PaginationBar);

  readonly users = signal<User[]>([]);
  readonly usersWithoutRoles = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly isLoadingWithoutRoles = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly isRefreshingWithoutRoles = signal<boolean>(false);
  readonly searchQuery = signal('');
  readonly searchQueryWithoutRoles = signal('');

  readonly nextPageToken = signal<string | null>(null);
  readonly nextPageTokenWithoutRoles = signal<string | null>(null);
  readonly dialog = inject(MatDialog);

  readonly expandedRoles = signal<Set<string>>(new Set<string>());

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

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  private langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.loadUsers();
      this.loadUsersWithoutRoles();
      this.loadOrganizations();
    });
  }

  ngOnDestroy(): void {
    if (this.langSubscription) {
      this.langSubscription.unsubscribe();
    }
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  onSearch(value: string) {
    this.searchQuery.set(value);
    this.pagination()?.reset();
    this.loadUsers();
  }

  onSearchWithoutRoles(value: string) {
    this.searchQueryWithoutRoles.set(value);
    this.paginationWithoutRoles()?.reset();
    this.loadUsersWithoutRoles();
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
        if (hasExistingRoles) {
          this.updateRoles(user.email, newRoles);
        } else {
          this.updateRolesForUserWithout(user.email, newRoles);
        }
      }
    });
  }

  updateRoles(email: string, roles: Role[]) {
    this.isLoading.set(true);
    this.#userService.updateRolesForUser(email, roles).subscribe({
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

  updateRolesForUserWithout(email: string, roles: Role[]) {
    this.isLoadingWithoutRoles.set(true);
    this.#userService.updateRolesForUserWithoutAny(email, roles).subscribe({
      next: () => {
        this.loadUsers();
        this.loadUsersWithoutRoles();

        this.isLoadingWithoutRoles.set(false);
      },
      error: (err) => {
        console.error('Error occured with updating roles', err);
        this.isLoadingWithoutRoles.set(false);
      },
    });
  }

  loadUsers(token?: string) {
    this.isLoading.set(true);

    this.#userService
      .getAllDatabaseUsers(ITEMS_PER_PAGE, token || undefined, this.searchQuery())
      .subscribe({
        next: (page) => {
          this.users.set(page.users);
          console.log(page);
          this.nextPageToken.set(page.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load users', err);
          this.isLoading.set(false);
        },
      });
  }

  loadUsersWithoutRoles(token?: string) {
    this.isLoadingWithoutRoles.set(true);

    this.#userService
      .getAllDatabaseUsersWithoutRoles(
        ITEMS_PER_PAGE,
        token || undefined,
        this.searchQueryWithoutRoles()
      )
      .subscribe({
        next: (page) => {
          this.usersWithoutRoles.set(page.users);
          this.nextPageTokenWithoutRoles.set(page.nextPageToken);
          this.isLoadingWithoutRoles.set(false);
        },
        error: (err) => {
          console.error('Failed to load users', err);
          this.isLoadingWithoutRoles.set(false);
        },
      });
  }

  loadOrganizations() {
    this.#orgService.getAllOrgs().subscribe({
      next: (orgs) => this.orgs.set(orgs),
      error: (err) => console.error('Fout bij laden organisaties', err),
    });
  }

  onOrganizationChange(user: User) {
    const newOrgId = user.organizationId;

    // Roep je UserService aan om de organisatie van de gebruiker te updaten
    this.#userService.updateUserOrg(user.email, newOrgId).subscribe({
      next: () => {
        // Eventueel een succesmelding of de lokale state updaten
        this.loadUsersWithoutRoles();
        this.loadUsers();
      },
    });
  }

  isOrgInList(orgId: number | null): boolean {
    if (!orgId) return false;
    return this.orgs().some((org) => org.id === orgId);
  }
}
