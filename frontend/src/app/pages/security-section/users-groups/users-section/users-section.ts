import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserOrgDetail } from '../../../../models/UserOrgDetails';
import { UserService } from '../../../../services/user-service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { UserOverviewResponse } from '../../../../models/UserOverviewResponse';
import { UsersPageWarnings } from '../../../../models/UsersPageWarnings';
import { SectionTopCard } from '../../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../../shared/AppIcons';

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

  orgUsers = signal<UserOrgDetail[]>([]);
  isLoading = signal(false);
  searchQuery = signal('');
  pageOverview = signal<UserOverviewResponse | null>(null);

  currentPage = signal(1);
  nextPageToken = signal<string | null>(null);

  hasWarnings = signal(false);
  userPageWarnings = signal<UsersPageWarnings>({
    twoFactorWarning: false,
    activeWithLongNoLogin: false,
    notActiveWithRecentLogin: false,
  });

  hasMultipleWarnings = computed(() => {
    const warnings = this.userPageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];
  #searchSubject = new Subject<string>();

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe((value) => {
      this.onSearch(value);
    });

    this.#loadPageOverview();
    this.#loadUsers();
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  onKeyup(value: string) {
    this.#searchSubject.next(value);
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
      const prevToken = this.#tokenHistory[this.#tokenHistory.length - 1];
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
