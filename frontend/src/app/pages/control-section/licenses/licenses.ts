import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { PageHeader } from '../../../components/page-header/page-header';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { BaseChartDirective } from 'ng2-charts';
import { Chart, ChartConfiguration, ChartData, ChartType, registerables } from 'chart.js';
import { LicenseService } from '../../../services/license-service';
import { LicenseType } from '../../../models/licenses/LicenseType';
import { InactiveUser } from '../../../models/licenses/InactiveUser';
import { LicenseOverviewResponse } from '../../../models/licenses/LicenseOverviewResponse';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { PageContentWrapper } from '../../../components/page-content-wrapper/page-content-wrapper';

@Component({
  selector: 'app-licenses',
  imports: [
    LucideAngularModule,
    PageHeader,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
    SectionTopCard,
    BaseChartDirective,
    PageWarnings,
    PageWarningsItem,
    TranslocoPipe,
    PageContentWrapper,
  ],
  templateUrl: './licenses.html',
  styleUrl: './licenses.css',
})
export class Licenses implements OnInit, OnDestroy {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly UtilityMethods = UtilityMethods;
  readonly #licenseService = inject(LicenseService);
  readonly #translocoService = inject(TranslocoService);

  constructor() {
    Chart.register(...registerables);
  }

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);
  readonly apiError = signal(false);

  readonly expandedDevice = signal<string | null>(null);

  readonly licenseTypes = signal<LicenseType[]>([]);
  readonly inactiveUsers = signal<InactiveUser[]>([]);
  readonly maxLicenseAmount = signal(0);
  readonly stepSize = signal(0);

  readonly pageOverview = signal<LicenseOverviewResponse | null>(null);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasWarnings = signal(false);

  readonly #colors = {
    toegewezenBlue: '#3b82f6', // Tailwind blue-500
    beschikbaarGrey: '#e5e7eb', // Tailwind gray-200
    enterpriseGreen: '#10b981', // Tailwind emerald-500
    voiceOrange: '#f59e0b', // Tailwind orange-500
  };

  // --- BAR CHART CONFIGURATION (LEFT) ---
  readonly barChartType: ChartType = 'bar';

  barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      {
        data: [],
        backgroundColor: this.#colors.toegewezenBlue,
        borderColor: this.#colors.toegewezenBlue,
        borderWidth: 0,
        borderRadius: 4, // Adds rounded corners to bars
      },
    ],
  };

  readonly barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      x: {
        grid: {
          display: false,
        },
        ticks: {
          callback: function (value: any) {
            const label = this.getLabelForValue(value);

            if (label.length > 15) {
              return label.substring(0, 15) + '...';
            }
            return label;
          },
          maxRotation: 0,
          minRotation: 0,
        },
      },
      y: {
        beginAtZero: true,
        grid: {
          color: '#f3f4f6',
        },
      },
    },
    plugins: {
      legend: {
        position: 'bottom',
        labels: {
          boxWidth: 15,
          color: '#6b7280',
          usePointStyle: false,
        },
      },
      title: { display: false },
    },
  };

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
      this.#loadLicenses();
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
  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  // ==========================================
  // PRIVATE METHODS
  // ==========================================
  #loadLicenses() {
    this.isLoading.set(true);
    this.apiError.set(false);

    this.#licenseService.getLicenses().subscribe({
      next: (res) => {
        this.licenseTypes.set(res.licenseTypes);
        this.inactiveUsers.set(res.inactiveUsers);
        this.maxLicenseAmount.set(res.maxLicenseAmount);
        this.stepSize.set(res.chartStepSize);

        this.#updateBarChart();

        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load licenses', err);
        this.apiError.set(true);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {
    this.#licenseService.getLicensesPageOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);

        if (this.pageOverview()?.riskyAccounts) {
          this.hasWarnings.set(true);
        }
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }

  #updateBarChart() {
    this.barChartData.labels = this.licenseTypes().map((l) => l.skuName);

    this.barChartData.datasets[0].data = this.licenseTypes().map((l) => l.totalAssigned);
    this.barChartData.datasets[0].label = this.#translocoService.translate('assigned');

    if (this.barChartOptions?.scales?.['y']) {
      const yScale = this.barChartOptions.scales['y'] as any;

      yScale.max = this.maxLicenseAmount();

      // Zorg dat het ticks object bestaat voordat je de stepSize zet
      if (!yScale.ticks) yScale.ticks = {};
      yScale.ticks.stepSize = this.stepSize();
    }

    this.barChartData = { ...this.barChartData };
  }
}
