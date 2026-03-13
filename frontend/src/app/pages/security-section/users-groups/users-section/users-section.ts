import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserOrgDetail } from '../../../../models/users/UserOrgDetails';
import { UserService } from '../../../../services/user-service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserOverviewResponse } from '../../../../models/users/UserOverviewResponse';
import { UsersPageWarnings } from '../../../../models/users/UsersPageWarnings';
import { SectionTopCard } from '../../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../../shared/AppIcons';
import { PageWarnings } from '../../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SearchBar } from '../../../../components/search-bar/search-bar';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-users-section',
  imports: [
    LucideAngularModule,
    SectionTopCard,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
    PageWarnings,
    PageWarningsItem,
    SearchBar,
  ],
  templateUrl: './users-section.html',
  styleUrl: './users-section.css',
})
export class UsersSection implements OnInit {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly #userService = inject(UserService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  readonly orgUsers = signal<UserOrgDetail[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly searchQuery = signal('');
  readonly pageOverview = signal<UserOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);

  readonly hasWarnings = signal(false);
  readonly userPageWarnings = signal<UsersPageWarnings>({
    twoFactorWarning: false,
    activeWithLongNoLogin: false,
    notActiveWithRecentLogin: false,
  });

  readonly hasMultipleWarnings = computed(() => {
    const warnings = this.userPageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#loadPageOverview();
    this.#loadUsers();
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.#loadUsers(null);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadUsers(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1);
      this.currentPage.update((p) => p - 1);
      this.#loadUsers(prevToken);
    }
  }

  getRoleClass(role: string) {
    switch (role) {
      case 'Super Admin':
        return 'bg-primary text-white';
      case 'Security Admin':
        return 'bg-purple-100 text-purple-700';
      case 'Regular User':
        return 'bg-blue-100 text-blue-700';
      case 'User Admin':
        return 'bg-fuchsia-100 text-fuchsia-700';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#userService.refreshUsersCache().subscribe({
      next: (res) => {
        console.log(res);

        this.currentPage.set(1);
        this.#tokenHistory = [null];

        this.#loadUsers(null);
        this.#loadPageOverview();
      },
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
      complete: () => {
        // Stop de spinner zodra alles klaar is
        this.isRefreshing.set(false);
      },
    });
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadUsers(token: string | null = null) {
    this.isLoading.set(true);

    this.#userService
      .getOrgUsers(ITEMS_PER_PAGE, token || undefined, this.searchQuery())
      .subscribe({
        next: (res) => {
          this.orgUsers.set(res.users);
          this.nextPageToken.set(res.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load users', err);
          this.isLoading.set(false);
        },
      });
  }

  #loadPageOverview() {
    this.#userService.getUsersPageOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);
        this.#loadWarnings();
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }

  #loadWarnings() {
    if (this.pageOverview()?.withoutTwoFactor! > 0) {
      this.hasWarnings.set(true);
      this.userPageWarnings().twoFactorWarning = true;
    }

    if (this.pageOverview()?.activeLongNoLoginCount! > 0) {
      this.hasWarnings.set(true);
      this.userPageWarnings().activeWithLongNoLogin = true;
    }

    if (this.pageOverview()?.inactiveRecentLoginCount! > 0) {
      this.hasWarnings.set(true);
      this.userPageWarnings().notActiveWithRecentLogin = true;
    }
  }
}
