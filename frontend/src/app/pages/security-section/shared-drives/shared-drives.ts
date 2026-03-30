import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { DriveService } from '../../../services/drive-service';
import { SearchBar } from '../../../components/search-bar/search-bar';
import { SharedDrive } from '../../../models/drives/SharedDrive';
import { SharedDrivesPageWarnings } from '../../../models/drives/SharedDrivesPageWarnings';
import { SharedDriveOverviewResponse } from '../../../models/drives/SharedDriveOverviewResponse';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { KPI_COLORS, kpiColors } from '../../../shared/KpiColors';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { PageContentWrapper } from '../../../components/page-content-wrapper/page-content-wrapper';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 2;

@Component({
  selector: 'app-shared-drives',
  imports: [
    PageHeader,
    SectionTopCard,
    SearchBar,
    LucideAngularModule,
    FormsModule,
    PageWarnings,
    PageWarningsItem,
    TranslocoPipe,
    PageContentWrapper,
  ],
  templateUrl: './shared-drives.html',
  styleUrl: './shared-drives.css',
})
export class SharedDrives implements OnInit, OnDestroy {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #driveService = inject(DriveService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);
  readonly #translocoService = inject(TranslocoService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);
  readonly apiError = signal(false);

  drives = signal<SharedDrive[]>([]);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly searchQuery = signal('');
  readonly pageOverview = signal<SharedDriveOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);

  readonly hasWarnings = computed(() => this.pageOverview()?.warnings?.hasWarnings ?? false);
  readonly hasMultipleWarnings = computed(
    () => this.pageOverview()?.warnings?.hasMultipleWarnings ?? false
  );
  readonly drivePageWarnings = computed((): SharedDrivesPageWarnings => {
    const items = this.pageOverview()?.warnings?.items ?? {};
    return {
      notOnlyDomainUsersAllowedWarning: items['notOnlyDomainUsersAllowedWarning'] ?? false,
      notOnlyMembersCanAccessWarning: items['notOnlyMembersCanAccessWarning'] ?? false,
      externalMembersWarning: items['externalMembersWarning'] ?? false,
      orphanDrivesWarning: items['orphanDrivesWarning'] ?? false,
    };
  });

  readonly kpiOrphanDrivesColors = computed(() =>
    kpiColors(
      this.pageOverview()?.orphanDrives ?? 0,
      this.#preferencesFacade.isDisabled('shared-drives', 'orphan'),
      KPI_COLORS.okBlue,
      KPI_COLORS.alertPurple
    )
  );

  readonly kpiExternalDrivesColors = computed(() =>
    kpiColors(
      this.pageOverview()?.externalMembersDriveCount ?? 0,
      this.#preferencesFacade.isDisabled('shared-drives', 'external'),
      KPI_COLORS.okBlue,
      KPI_COLORS.alertOrange
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
      this.#loadDrives();
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
    this.#loadDrives(null);
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadDrives(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1);
      this.currentPage.update((p) => p - 1);
      this.#loadDrives(prevToken);
    }
  }

  isSharedDrivePrefDisabled(key: 'outsideDomain' | 'nonMemberAccess'): boolean {
    return this.#preferencesFacade.isDisabled('shared-drives', key);
  }

  openSecurityScoreDetail() {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(overview?.securityScore ?? 0, 'drives');
    this.#securityScoreDetail.open(breakdown, 'drives');
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#driveService.refreshDriveCache().subscribe({
      next: (res) => {
        this.currentPage.set(1);
        this.#tokenHistory = [null];

        this.#loadDrives(null);
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
  #loadDrives(token: string | null = null) {
    this.isLoading.set(true);
    this.apiError.set(false);

    this.#driveService.getDrives(ITEMS_PER_PAGE, token || undefined, this.searchQuery()).subscribe({
      next: (res) => {
        const mappedDrives = (res.drives || []).map((d) => ({ ...d, isLoadingDetails: true }));

        this.drives.set(mappedDrives);
        this.nextPageToken.set(res.nextPageToken);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load shared drives', err);
        this.isLoading.set(false);
        this.apiError.set(true);
      },
    });
  }

  #loadPageOverview() {
    this.#preferencesFacade.loadWithPrefs$(this.#driveService.getDrivesPageOverview()).subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: (err) => console.error('Failed to load page overview', err),
    });
  }
}
