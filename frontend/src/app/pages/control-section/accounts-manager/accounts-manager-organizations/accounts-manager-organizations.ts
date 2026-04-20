import { CommonModule } from '@angular/common';
import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { PaginationBar } from '../../../../components/pagination-bar/pagination-bar';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { AccountsManagerTable } from '../accounts-manager-table/accounts-manager-table';
import { Role, RoleLabels, RolePriority, User } from '../../../../models/users/User';
import { MatDialog } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { AssignRoleDialog } from '../../../../components/assign-role-dialog/assign-role-dialog';
import { AccountSectionType } from '../../../../models/AccountSectionType';
import { Organization } from '../../../../models/org/Organization';
import { OrgService } from '../../../../services/org-service';
import { UserService } from '../../../../services/user-service';
import { AppIcons } from '../../../../shared/AppIcons';

const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-accounts-manager-organizations',
  imports: [
    TranslocoPipe,
    SearchBar,
    PaginationBar,
    LucideAngularModule,
    FormsModule,
    AccountsManagerTable,
    CommonModule,
  ],
  templateUrl: './accounts-manager-organizations.html',
  styleUrl: './accounts-manager-organizations.css',
})
export class AccountsManagerOrganizations {
  readonly Icons = AppIcons;
  readonly #userService = inject(UserService);
  readonly #translocoService = inject(TranslocoService);
  readonly #orgService = inject(OrgService);

  readonly pagination = viewChild(PaginationBar);

  readonly orgs = signal<Organization[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly searchQuery = signal('');

  readonly nextPageToken = signal<string | null>(null);

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  private langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
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
    this.loadOrganizations();
  }

  loadOrganizations(token?: string) {
    this.isLoading.set(true);

    this.#orgService
      .getAllOrgsPaged(ITEMS_PER_PAGE, token || undefined, this.searchQuery())
      .subscribe({
        next: (page) => {
          this.orgs.set(page.organizations);
          this.nextPageToken.set(page.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Fout bij laden organisaties', err);
          this.isLoading.set(false);
        },
      });
  }
}
