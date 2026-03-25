import { Component, inject, OnInit, signal, computed, OnDestroy } from '@angular/core';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { FilterChips } from '../../../components/filter-chips/filter-chips';
import { SearchBar } from '../../../components/search-bar/search-bar';
import { OAuthService } from '../../../services/o-auth-service';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { AggregatedAppDto } from '../../../models/o-auth/AggregatedAppDto';
import { OAuthOverviewResponse } from '../../../models/o-auth/OAuthOverviewResponse';
import { Risk } from '../../../models/o-auth/Risk';
import { FilterOption } from '../../../models/FilterOption';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 3;

@Component({
  selector: 'app-app-access',
  imports: [
    PageHeader,
    SectionTopCard,
    FilterChips,
    SearchBar,
    LucideAngularModule,
    FormsModule,
    TranslocoPipe,
  ],
  templateUrl: './app-access.html',
  styleUrl: './app-access.css',
})
export class AppAccess implements OnInit, OnDestroy {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #oAuthService = inject(OAuthService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  readonly apps = signal<AggregatedAppDto[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly searchQuery = signal('');
  readonly pageOverview = signal<OAuthOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);

  readonly expandedApp = signal<string | null>(null);

  readonly riskFilter = signal<Risk>('all');

  readonly allFilteredApps = signal(0);
  readonly allHighRiskApps = signal(0);
  readonly allNotHighRiskApps = signal(0);

  readonly highRiskAlertsEnabled = computed(
    () => !this.#preferencesFacade.isDisabled('app-access', 'highRisk'),
  );

  readonly kpiHighRiskAppsColors = computed(() => {
    const o = this.pageOverview();
    const n = o?.totalHighRiskApps ?? 0;
    if (n === 0) return { bg: '#dbeafe', icon: '#155dfc', text: 'black' };
    if (!this.highRiskAlertsEnabled()) {
      return { bg: '#f3f4f6', icon: '#6b7280', text: '#6b7280' };
    }
    return { bg: '#ffe2e2', icon: '#e7000b', text: '#e7000b' };
  });

  readonly filterOptions = computed<FilterOption[]>(() => [
    {
      value: 'all',
      label: 'Alle apps',
      count: this.allFilteredApps(),
      activeClass: 'bg-[#3ABFAD] text-white',
      inactiveClass: '',
    },
    {
      value: 'high',
      label: 'Hoog risico',
      count: this.allHighRiskApps(),
      activeClass: 'bg-red-100 text-red-800',
      inactiveClass: '',
    },
    {
      value: 'not-high',
      label: 'Geen risico',
      count: this.allNotHighRiskApps(),
      activeClass: 'bg-emerald-100 text-emerald-800',
      inactiveClass: '',
    },
  ]);

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
      this.#loadApps();
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
  toggleExpanded(): void {
    this.isExpanded.update((v) => !v);
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.#resetData();
  }

  nextPage(): void {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadApps(token);
    }
  }

  prevPage(): void {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1);
      this.currentPage.update((p) => p - 1);
      this.#loadApps(prevToken);
    }
  }

  toggleExpand(deviceId: string): void {
    if (this.expandedApp() === deviceId) {
      this.expandedApp.set(null);
    } else {
      this.expandedApp.set(deviceId);
    }
  }

  refreshData(): void {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#oAuthService.refreshOAuthCache().subscribe({
      next: (res) => {
        this.#resetData();
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

  setRiskFilter(risk: string): void {
    this.riskFilter.set(risk as Risk);
    this.#resetData();
  }

  openSecurityScoreDetail(): void {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(overview?.securityScore ?? 0, 'app-access');
    this.#securityScoreDetail.open(breakdown, 'app-access');
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadApps(token: string | null = null): void {
    this.isLoading.set(true);

    this.#oAuthService
      .getApps(ITEMS_PER_PAGE, this.riskFilter(), token || undefined, this.searchQuery())
      .subscribe({
        next: (res) => {
          this.apps.set(res.apps);
          this.nextPageToken.set(res.nextPageToken);
          this.allFilteredApps.set(res.allFilteredApps);
          this.allHighRiskApps.set(res.allHighRiskApps);
          this.allNotHighRiskApps.set(res.allNotHighRiskApps);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load oAuth apps', err);
          this.isLoading.set(false);
        },
      });
  }

  #loadPageOverview(): void {
    forkJoin({
      overview: this.#oAuthService.getOAuthPageOverview(),
      _: this.#preferencesFacade.loadDisabled$(),
    }).subscribe({
      next: ({ overview }) => {
        this.pageOverview.set(overview);
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }

  #resetData(): void {
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.#loadApps(null);
  }
}
