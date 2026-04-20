import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../components/page-header/page-header';
import { SectionTopCard } from '../../components/section-top-card/section-top-card';
import { AppIcons } from '../../shared/AppIcons';
import { UtilityMethods } from '../../shared/UtilityMethods';
import { DashboardService } from '../../services/dashboard-service';
import { LucideAngularModule } from 'lucide-angular';
import { Router } from '@angular/router';
import { SecurityGauge } from '../../components/security-gauge/security-gauge';
import { SecurityComponent } from '../../components/security-component/security-component';
import { DashboardPageResponse } from '../../models/dashboard/DashboardPageResponse';
import { DashboardOverviewResponse } from '../../models/dashboard/DashboardOverviewResponse';
import { PageWarnings } from '../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../components/page-warnings/page-warnings-item/page-warnings-item';
import { ReportService } from '../../services/report-service';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { PageContentWrapper } from '../../components/page-content-wrapper/page-content-wrapper';
import { ApiError } from '../../components/api-error/api-error';

@Component({
  selector: 'app-home',
  imports: [
    PageHeader,
    SectionTopCard,
    LucideAngularModule,
    SecurityGauge,
    SecurityComponent,
    PageWarnings,
    PageWarningsItem,
    TranslocoPipe,
    PageContentWrapper,
    ApiError,
  ],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit, OnDestroy {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #dashboardService = inject(DashboardService);
  readonly #reportService = inject(ReportService);
  readonly #router = inject(Router);

  readonly #translocoService = inject(TranslocoService);

  private langSubscription?: Subscription;

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isLoading = signal(false);
  readonly isGenerating = signal<boolean>(false);

  readonly hasWarnings = signal(false);

  readonly apiError = signal(false);
  readonly errorMessage = signal<string | null>(null);

  pageResponse = signal<DashboardPageResponse | null>(null);
  pageOverview = signal<DashboardOverviewResponse | null>(null);

  score = signal<number>(0);

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadDashboardData();
    });

    this.#loadPageOverview();
  }

  ngOnDestroy(): void {
    if (this.langSubscription) {
      this.langSubscription.unsubscribe();
    }
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================

  routeToPage(link: string) {
    this.#router.navigate([link]);
  }

  generateRapport() {
    if (this.isGenerating()) return;

    this.isGenerating.set(true);

    this.#reportService.downloadSecurityRapport().subscribe({
      next: (blob: Blob) => {
        const url = globalThis.URL.createObjectURL(blob);

        const link = document.createElement('a');
        link.href = url;
        link.download = `Security_Report_${'CLOUDMEN_Labo'}.pdf`;

        link.click();

        globalThis.URL.revokeObjectURL(url);
        this.isGenerating.set(false);
      },
      error: (err) => {
        console.error('Download failed', err);
        alert('Could not generate PDF. Please try again.');
        this.isGenerating.set(false);
      },
    });
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadDashboardData() {
    this.isLoading.set(true);

    this.#dashboardService.getDashboardData().subscribe({
      next: (res) => {
        this.pageResponse.set(res);
        this.score.set(res.overallScore);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.error);
        console.error('Failed to load dashboard data', err);
        this.apiError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {
    this.#dashboardService.getDashboardPageOverview().subscribe({
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
    if (this.pageOverview()?.criticalNotifications! > 0) {
      this.hasWarnings.set(true);
    }
  }
}
