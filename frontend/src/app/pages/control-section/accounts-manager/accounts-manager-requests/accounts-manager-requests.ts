import { Component, inject, signal, viewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { AssignRoleDialog } from '../../../../components/assign-role-dialog/assign-role-dialog';
import { PaginationBar } from '../../../../components/pagination-bar/pagination-bar';
import { Organization } from '../../../../models/org/Organization';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { OrgService } from '../../../../services/org-service';
import { UserService } from '../../../../services/user-service';
import { AppIcons } from '../../../../shared/AppIcons';
import { Router } from '@angular/router';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { AccountsManagerTable } from '../accounts-manager-table/accounts-manager-table';
import { PageHeader } from '../../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';
import { AccountsManagerRequestsTable } from './accounts-manager-requests-table/accounts-manager-requests-table';
import { AccessDecisionDialog } from '../../../../components/access-decision-dialog/access-decision-dialog';

const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-accounts-manager-requests',
  imports: [
    SearchBar,
    AccountsManagerTable,
    PaginationBar,
    TranslocoPipe,
    PageHeader,
    LucideAngularModule,
    AccountsManagerRequestsTable,
  ],
  templateUrl: './accounts-manager-requests.html',
  styleUrl: './accounts-manager-requests.css',
})
export class AccountsManagerRequests {
  readonly Icons = AppIcons;
  readonly #userService = inject(UserService);
  readonly #translocoService = inject(TranslocoService);
  readonly #orgService = inject(OrgService);
  readonly #router = inject(Router);

  readonly pagination = viewChild(PaginationBar);

  readonly usersWithRequests = signal<User[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly searchQuery = signal('');

  readonly nextPageToken = signal<string | null>(null);
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

  readonly selectedOrganization = signal<string>('');
  readonly uniqueOrganizations = signal<{ id: string; name: string }[]>([
    { id: '', name: 'account-manager.all-orgs' },
  ]); // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  private langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.loadUsersWithRequests();
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
    this.loadUsersWithRequests();
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

  openAcceptedDialog(user: User): void {
    const dialogRef = this.dialog.open(AccessDecisionDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isAccepted: true,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // has been accepted
        this.#userService.userAccepted(user).subscribe({
          next: () => {
            this.openRoleChangeDialog(user);
          },
          error: (err) => {
            console.error('Error with accepting user', err);
          },
        });
      }
    });
  }

  openDenyDialog(user: User): void {
    const dialogRef = this.dialog.open(AccessDecisionDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isAccepted: false,
      },
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        // has been denied
      }
    });
  }

  openRoleChangeDialog(user: User): void {
    const dialogRef = this.dialog.open(AssignRoleDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isEditMode: false,
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
    this.#userService.updateRolesForUser(email, roles).subscribe({
      next: () => {
        this.loadUsersWithRequests();
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Error occured with updating roles', err);
        this.isLoading.set(false);
      },
    });
  }

  loadUsersWithRequests(token?: string) {
    this.isLoading.set(true);

    this.#userService
      .getAllRequestedDatabaseUsers(ITEMS_PER_PAGE, token || undefined, this.searchQuery())
      .subscribe({
        next: (page) => {
          this.usersWithRequests.set(page.users);
          console.log(page);
          this.nextPageToken.set(page.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load requested users', err);
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
    this.#userService.updateUserOrg(user.email, newOrgId).subscribe({
      next: () => {
        // Eventueel een succesmelding of de lokale state updaten
        this.loadUsersWithRequests();
      },
    });
  }

  isOrgInList(orgId: number | null): boolean {
    if (!orgId) return false;
    return this.orgs().some((org) => org.id === orgId);
  }

  #resetAndLoad() {
    this.pagination()?.reset();

    this.loadUsersWithRequests();
  }

  backToAccountManagement() {
    this.#router.navigate(['/accounts-manager']);
  }
}
