import { Component, inject, OnInit, signal } from '@angular/core';
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

@Component({
  selector: 'app-home',
  imports: [PageHeader, SectionTopCard, LucideAngularModule, SecurityGauge, SecurityComponent],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #dashboardService = inject(DashboardService);
  readonly #router = inject(Router);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasWarnings = signal(false);

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
    this.#loadPageOverview();
    this.#loadDashboardData();
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  navigateToNotifications() {
    this.#router.navigate(['/reports-reactions']);
  }

  routeToPage(link: string) {
    this.#router.navigate([link]);
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#loadDashboardData();
    this.#loadPageOverview();

    this.isRefreshing.set(false);
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
        console.error('Failed to load dashboard data', err);
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
