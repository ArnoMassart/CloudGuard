import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { PageHeader } from '../../../components/page-header/page-header';
import { AppPasswordsService } from '../../../services/app-password-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { AppPassword } from '../../../models/app-password/AppPassword';
import { AppPasswordOverviewResponse } from '../../../models/app-password/AppPasswordOverviewResponse';
import { UserAppPasswords } from '../../../models/app-password/UserAppPasswords';

import { LucideAngularModule } from 'lucide-angular';
import { Subject, Subscription } from 'rxjs';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SearchBar } from '../../../components/search-bar/search-bar';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { KPI_COLORS, kpiColors } from '../../../shared/KpiColors';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-app-passwords',
  imports: [
    SectionTopCard,
    CommonModule,
    PageHeader,
    SearchBar,
    LucideAngularModule,
    PageWarnings,
    PageWarningsItem,
    TranslocoPipe,
  ],
  templateUrl: './app-passwords.html',
  styleUrl: './app-passwords.css',
})
export class AppPasswords implements OnInit, OnDestroy {
  readonly Icons = AppIcons;
  readonly pageOverview = signal<AppPasswordOverviewResponse | null>(null);
  readonly #appPasswordsService = inject(AppPasswordsService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

  readonly userAppPasswords = signal<UserAppPasswords[]>([]);
  readonly expandedAppPassword = signal<string | null>(null);
  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal(false);
  readonly loadError = signal(false);
  readonly searchQuery = signal('');
  readonly filteredUserAppPasswords = computed(() => {
    const users = this.userAppPasswords();
    const q = this.searchQuery().toLowerCase().trim();
    if (!q) return users;
    return users.filter(
      (u) => u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
    );
  });
  readonly #searchSubject = new Subject<string>();
  readonly isExpanded = signal(true);

  readonly appPasswordAlertsEnabled = computed(
    () => !this.#preferencesFacade.isDisabled('app-passwords', 'appPassword'),
  );

  readonly kpiAppPasswordAllowedColors = computed(() =>
    kpiColors(
      this.pageOverview()?.allowed ? 1 : 0,
      !this.appPasswordAlertsEnabled(),
      KPI_COLORS.okGreen, KPI_COLORS.alertRed,
    )
  );

  readonly kpiAppPasswordTotalColors = computed(() =>
    kpiColors(
      this.pageOverview()?.totalAppPasswords ?? 0,
      !this.appPasswordAlertsEnabled(),
      KPI_COLORS.okBlue, KPI_COLORS.alertRed,
    )
  );

  #tokenHistory: (string | null)[] = [null];
  #langSubscription?: Subscription;

  ngOnInit(): void {
    this.#preferencesFacade.loadWithPrefs$(this.#appPasswordsService.getOverview()).subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: () => {},
    });
    this.#loadAppPasswords(null);
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#preferencesFacade.loadWithPrefs$(this.#appPasswordsService.getOverview()).subscribe({
        next: (overview) => this.pageOverview.set(overview),
        error: () => {},
      });
      this.#loadAppPasswords(null);
    });
  }

  ngOnDestroy(): void {
    if (this.#langSubscription) {
      this.#langSubscription.unsubscribe();
    }
  }

  onSearch(value: string) {
    this.searchQuery.set(value);
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.#loadAppPasswords(null);
  }

  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadAppPasswords(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1) ?? null;
      this.currentPage.update((p) => p - 1);
      this.#loadAppPasswords(prevToken);
    }
  }

  toggleExpand(email: string) {
    if (this.expandedAppPassword() === email) {
      this.expandedAppPassword.set(null);
    } else {
      this.expandedAppPassword.set(email);
    }
  }

  getAdminUserUrl(user: UserAppPasswords): string {
    const id = user.id?.trim();
    if (!id) return 'https://admin.google.com/u/1/ac/users';
    return `https://admin.google.com/u/1/ac/users/${encodeURIComponent(id)}`;
  }

  retryLoad() {
    this.#tokenHistory = [null];
    this.currentPage.set(1);
    this.#loadAppPasswords(null);
  }

  refreshData() {
    if (this.isRefreshing()) return;
    this.isRefreshing.set(true);
    this.#appPasswordsService.refreshCache().subscribe({
      next: () => {
        this.#appPasswordsService.getOverview().subscribe({
          next: (overview) => this.pageOverview.set(overview),
          error: () => {},
        });
        this.#tokenHistory = [null];
        this.currentPage.set(1);
        this.#loadAppPasswords(null);
      },
      error: (err) => {
        console.error('Kon cache niet vernieuwen:', err);
        this.isRefreshing.set(false);
      },
      complete: () => {
        this.isRefreshing.set(false);
      },
    });
  }

  #loadAppPasswords(pageToken: string | null) {
    this.isLoading.set(true);
    this.loadError.set(false);
    this.expandedAppPassword.set(null);
    this.#appPasswordsService
      .getAppPasswords(ITEMS_PER_PAGE, pageToken ?? undefined, this.searchQuery())
      .subscribe({
        next: (response) => {
          const data = response.users.map((u) => this.#mapToUserAppPasswords(u));
          this.userAppPasswords.set(data);
          this.nextPageToken.set(response.nextPageToken ?? null);
          this.isLoading.set(false);
        },
        error: () => {
          this.userAppPasswords.set([]);
          this.nextPageToken.set(null);
          this.loadError.set(true);
          this.isLoading.set(false);
        },
      });
  }

  #mapToUserAppPasswords(u: {
    id: string;
    name: string;
    email: string;
    role: string;
    tsv: boolean;
    passwords: AppPassword[];
  }): UserAppPasswords {
    return {
      id: u.id ?? u.email,
      name: u.name,
      email: u.email,
      role: u.role,
      twoFactorEnabled: u.tsv,
      appPasswords: u.passwords,
    };
  }

  formatDate(value: Date | string | null): string {
    if (!value) return '–';
    const d = typeof value === 'string' ? new Date(Number(value) || value) : value;
    if (Number.isNaN(d.getTime())) return '–';
    return d.toLocaleDateString('nl-NL', { day: 'numeric', month: 'numeric', year: 'numeric' });
  }

  formatLastUsed(value: Date | string | null): string {
    if (!value) return 'nooit';
    const d = typeof value === 'string' ? new Date(Number(value) || value) : value;
    if (Number.isNaN(d.getTime())) return 'nooit';
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'vandaag';
    if (diffDays === 1) return 'gisteren';
    if (diffDays < 7) return `${diffDays} dagen geleden`;
    if (diffDays < 31) return `${Math.floor(diffDays / 7)} weken geleden`;
    return `${Math.floor(diffDays / 31)} maanden geleden`;
  }

  openSecurityScoreDetail(): void {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(
        overview?.securityScore ?? 0,
        'App Wachtwoorden'
      );
    this.#securityScoreDetail.open(breakdown, 'app-passwords');
  }
}
