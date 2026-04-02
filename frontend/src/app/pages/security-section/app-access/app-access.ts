import { Component, inject, OnInit, signal, computed, OnDestroy, viewChild } from '@angular/core';
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
import { KPI_COLORS, kpiColors } from '../../../shared/KpiColors';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { PageContentWrapper } from '../../../components/page-content-wrapper/page-content-wrapper';
import { PaginationBar } from '../../../components/pagination-bar/pagination-bar';

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
    PageContentWrapper,
    PaginationBar,
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

  readonly pagination = viewChild(PaginationBar);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);
  readonly apiError = signal(false);

  readonly apps = signal<AggregatedAppDto[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly searchQuery = signal('');
  readonly pageOverview = signal<OAuthOverviewResponse | null>(null);

  readonly nextPageToken = signal<string | null>(null);

  readonly expandedApp = signal<string | null>(null);

  readonly riskFilter = signal<Risk>('all');

  readonly allFilteredApps = signal(0);
  readonly allHighRiskApps = signal(0);
  readonly allNotHighRiskApps = signal(0);

  readonly highRiskAlertsEnabled = computed(
    () => !this.#preferencesFacade.isDisabled('app-access', 'highRisk')
  );

  readonly kpiHighRiskAppsColors = computed(() =>
    kpiColors(
      this.pageOverview()?.totalHighRiskApps ?? 0,
      !this.highRiskAlertsEnabled(),
      KPI_COLORS.okBlue,
      KPI_COLORS.alertRed
    )
  );

  readonly filterOptions = computed<FilterOption[]>(() => {
    const highRiskChipMuted = !this.highRiskAlertsEnabled();
    return [
      {
        value: 'all',
        label: 'all-apps',
        count: this.allFilteredApps(),
        activeClass: 'bg-[#3ABFAD] text-white',
        inactiveClass: '',
      },
      {
        value: 'high',
        label: 'high-risk',
        count: this.allHighRiskApps(),
        activeClass: highRiskChipMuted ? 'bg-[#f3f4f6] text-[#6b7280]' : 'bg-red-100 text-red-800',
        inactiveClass: '',
      },
      {
        value: 'not-high',
        label: 'no-risk',
        count: this.allNotHighRiskApps(),
        activeClass: 'bg-emerald-100 text-emerald-800',
        inactiveClass: '',
      },
    ];
  });

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  private langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadPageOverview();
      this.loadApps();
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
  toggleExpanded(): void {
    this.isExpanded.update((v) => !v);
  }

  onSearch(value: string): void {
    this.searchQuery.set(value);
    this.#resetData();
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
  loadApps(token?: string): void {
    this.isLoading.set(true);
    this.apiError.set(false);

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
          this.apiError.set(true);
          this.isLoading.set(false);
        },
      });
  }

  #loadPageOverview(): void {
    this.#preferencesFacade.loadWithPrefs$(this.#oAuthService.getOAuthPageOverview()).subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: (err) => console.error('Failed to load page overview', err),
    });
  }

  #resetData(): void {
    this.pagination()?.reset();
    this.loadApps();
  }
}
