import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { USER_SECURITY_VIOLATION, UserOrgDetail } from '../../../../models/users/UserOrgDetails';
import { UserService } from '../../../../services/user-service';
import { SecurityScoreDetailService } from '../../../../services/security-score-detail.service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserOverviewResponse } from '../../../../models/users/UserOverviewResponse';
import { UsersPageWarnings } from '../../../../models/users/UsersPageWarnings';
import { SectionTopCard } from '../../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../../shared/AppIcons';
import { PageWarnings } from '../../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SearchBar } from '../../../../components/search-bar/search-bar';
import { SecurityPreferencesFacade } from '../../../../services/security-preferences-facade';
import { KPI_COLORS, kpiColors, evaluateWarnings } from '../../../../shared/KpiColors';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

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
    TranslocoPipe,
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
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

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
    twoFactorWarning: true,
    activeWithLongNoLogin: true,
    notActiveWithRecentLogin: true,
  });

  readonly hasMultipleWarnings = computed(() => {
    const warnings = this.userPageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

  readonly kpiZonder2faColors = computed(() =>
    kpiColors(
      this.pageOverview()?.withoutTwoFactor ?? 0,
      this.#preferencesFacade.isDisabled('users-groups', '2fa'),
      KPI_COLORS.okBlue, KPI_COLORS.alertOrange,
    )
  );

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];
  #langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadPageOverview();
      this.#loadUsers();
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
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

  /** 2FA off and 2FA warning muted in preferences → gray cell (Security column unchanged). */
  twoFactorCellMuted(user: UserOrgDetail): boolean {
    return (
      !user.twoFactorEnabled && this.#preferencesFacade.isDisabled('users-groups', '2fa')
    );
  }

  /** Activity warning muted and user has an activity violation → gray last-login cell. */
  lastLoginCellMuted(user: UserOrgDetail): boolean {
    if (!this.#preferencesFacade.isDisabled('users-groups', 'activity')) {
      return false;
    }
    const codes = user.securityViolationCodes ?? [];
    return codes.some(
      (c) =>
        c === USER_SECURITY_VIOLATION.ACTIVITY_STALE ||
        c === USER_SECURITY_VIOLATION.ACTIVITY_INACTIVE_RECENT,
    );
  }

  /**
   * Shows Conform when the user is already conform, or when every violation is covered by a disabled (muted) preference.
   */
  effectiveSecurityConform(user: UserOrgDetail): boolean {
    if (user.securityConform) {
      return true;
    }
    const codes = user.securityViolationCodes;
    if (!codes?.length) {
      return false;
    }
    return codes.every((code) => {
      if (code === USER_SECURITY_VIOLATION.NO_2FA) {
        return this.#preferencesFacade.isDisabled('users-groups', '2fa');
      }
      if (
        code === USER_SECURITY_VIOLATION.ACTIVITY_STALE ||
        code === USER_SECURITY_VIOLATION.ACTIVITY_INACTIVE_RECENT
      ) {
        return this.#preferencesFacade.isDisabled('users-groups', 'activity');
      }
      return false;
    });
  }

  openSecurityScoreDetail() {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(overview?.securityScore ?? 0, 'users');
    this.#securityScoreDetail.open(breakdown, 'users');
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#userService.refreshUsersCache().subscribe({
      next: () => {
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

    this.#userService.getOrgUsers(ITEMS_PER_PAGE, token || undefined, this.searchQuery()).subscribe({
      next: (page) => {
        this.orgUsers.set(page.users);
        this.nextPageToken.set(page.nextPageToken);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {
    this.#preferencesFacade.loadWithPrefs$(this.#userService.getUsersPageOverview()).subscribe({
      next: (overview) => {
        this.pageOverview.set(overview);
        this.#loadWarnings();
      },
      error: (err) => console.error('Failed to load page overview', err),
    });
  }

  #loadWarnings() {
    const o = this.pageOverview();
    if (!o) return;
    const { warnings, hasWarnings } = evaluateWarnings(
      [
        { key: 'twoFactorWarning' as const, count: o.withoutTwoFactor ?? 0, section: 'users-groups', prefKey: '2fa' },
        { key: 'activeWithLongNoLogin' as const, count: o.activeLongNoLoginCount ?? 0, section: 'users-groups', prefKey: 'activity' },
        { key: 'notActiveWithRecentLogin' as const, count: o.inactiveRecentLoginCount ?? 0, section: 'users-groups', prefKey: 'activity' },
      ],
      (s, k) => this.#preferencesFacade.isDisabled(s, k),
    );
    this.userPageWarnings.set(warnings);
    this.hasWarnings.set(hasWarnings);
  }
}
