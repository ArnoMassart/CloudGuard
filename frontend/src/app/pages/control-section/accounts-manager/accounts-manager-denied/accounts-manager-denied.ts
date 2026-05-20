import { Component, inject, input, output, signal, viewChild } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../../components/page-header/page-header';
import { PaginationBar } from '../../../../components/pagination-bar/pagination-bar';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { AccountsManagerRequestsTable } from '../accounts-manager-requests/accounts-manager-requests-table/accounts-manager-requests-table';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { Organization } from '../../../../models/org/Organization';
import { AppIcons } from '../../../../shared/AppIcons';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { AssignRoleDialog } from '../../../../components/assign-role-dialog/assign-role-dialog';
import { UserDecisionDialog } from '../../../../components/user-decision-dialog/user-decision-dialog';
import { DecisionResult } from '../../../../models/DecisionResult';
import { OrgService } from '../../../../services/org-service';
import { UserService } from '../../../../services/user-service';
import { Router } from '@angular/router';
import { UserDenyRequest } from '../../../../models/users/UserDenyRequest';
import { AccountsManagerDeniedTable } from './accounts-manager-denied-table/accounts-manager-denied-table';
import { DeniedUser } from '../../../../models/users/DeniedUser';
import { ResetDenialDialog } from '../../../../components/reset-denial-dialog/reset-denial-dialog';
import { ResetDenialResult } from '../../../../models/users/ResetDenialResult';

const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-accounts-manager-denied',
  imports: [
    SearchBar,
    PaginationBar,
    TranslocoPipe,
    PageHeader,
    LucideAngularModule,
    AccountsManagerDeniedTable,
  ],
  templateUrl: './accounts-manager-denied.html',
  styleUrl: './accounts-manager-denied.css',
})
export class AccountsManagerDenied {
  readonly Icons = AppIcons;
  readonly #userService = inject(UserService);
  readonly #translocoService = inject(TranslocoService);
  readonly #orgService = inject(OrgService);
  readonly #router = inject(Router);

  readonly pagination = viewChild(PaginationBar);

  readonly usersDenied = signal<DeniedUser[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly searchQuery = signal('');

  readonly nextPageToken = signal<string | null>(null);
  readonly dialog = inject(MatDialog);

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
    const dialogRef = this.dialog.open(UserDecisionDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isAccepted: true,
        uniqueOrganizations: this.orgs(),
        regularRoles: this.getAllAvailableRoles(),
      },
    });

    dialogRef.afterClosed().subscribe((result: DecisionResult) => {
      if (result && result.isAccepted) {
        // has been accepted
        this.#userService.userAccepted(result).subscribe({
          next: () => {
            this.#resetAndLoad();
          },
          error: (err) => {
            console.error('Error with accepting user', err);
          },
        });
      }
    });
  }

  openDenyDialog(user: User): void {
    const dialogRef = this.dialog.open(UserDecisionDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
        isAccepted: false,
        uniqueOrganizations: this.orgs(),
        regularRoles: this.getAllAvailableRoles(),
      },
    });

    dialogRef.afterClosed().subscribe((result: DecisionResult) => {
      if (result && !result.isAccepted) {
        const request: UserDenyRequest = {
          userEmail: result.userEmail,
          denyReason: result.denyReason,
        };

        this.#userService.userDenied(request).subscribe({
          next: () => {
            this.#resetAndLoad();
          },
          error: (err) => {
            console.error('Error with denying user', err);
          },
        });
      }
    });
  }

  openReacceptDialog(user: DeniedUser): void {
    const dialogRef = this.dialog.open(ResetDenialDialog, {
      width: '450px',
      panelClass: 'custom-dialog-container',
      data: {
        user: user,
      },
    });

    dialogRef.afterClosed().subscribe((result: ResetDenialResult) => {
      if (result && result.confirmed) {
        this.#userService.userReaccepted(result.userEmail).subscribe({
          next: () => {
            this.#resetAndLoad();
          },
          error: (err) => {
            console.error('Error with accepting user', err);
          },
        });
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
      .getAllDeniedDatabaseUsers(ITEMS_PER_PAGE, token || undefined, this.searchQuery())
      .subscribe({
        next: (page) => {
          this.usersDenied.set(page.users);
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
