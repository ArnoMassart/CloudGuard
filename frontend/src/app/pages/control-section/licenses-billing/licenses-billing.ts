import { Component, inject, signal } from '@angular/core';
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
import { window } from 'rxjs';

@Component({
  selector: 'app-licenses-billing',
  imports: [
    LucideAngularModule,
    PageHeader,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
    SectionTopCard,
    BaseChartDirective,
  ],
  templateUrl: './licenses-billing.html',
  styleUrl: './licenses-billing.css',
})
export class LicensesBilling {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly #licenseService = inject(LicenseService);
  readonly UtilityMethods = UtilityMethods;

  constructor() {
    Chart.register(...registerables);
  }

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  readonly expandedDevice = signal<string | null>(null);

  readonly licenseTypes = signal<LicenseType[]>([]);
  readonly inactiveUsers = signal<InactiveUser[]>([]);
  readonly maxLicenseAmount = signal(0);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasCostsSaved = signal(true);

  // Custom Colors Matching the Image
  private colors = {
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
        label: 'Toegewezen',
        backgroundColor: this.colors.toegewezenBlue,
        borderColor: this.colors.toegewezenBlue,
        borderWidth: 0,
        borderRadius: 4, // Adds rounded corners to bars
      },
      {
        data: [],
        label: 'Beschikbaar',
        backgroundColor: this.colors.beschikbaarGrey,
        borderColor: this.colors.beschikbaarGrey,
        borderWidth: 0,
        borderRadius: 4,
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
      },
      y: {
        beginAtZero: true,
        ticks: {
          stepSize: 15,
        },
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

  // --- PIE CHART CONFIGURATION (RIGHT) ---
  readonly pieChartType: ChartType = 'pie';

  readonly pieChartData: ChartData<'pie'> = {
    // Re-order labels to match the clockwise flow: Blue -> Green -> Orange
    labels: ['Business Standard: 57%', 'Enterprise: 38%', 'Google Voice: 5%'],
    datasets: [
      {
        data: [57, 38, 5],
        backgroundColor: [
          '#3b82f6', // Blue
          '#10b981', // Green
          '#f59e0b', // Orange/Yellow
        ],
        borderWidth: 2,
        borderColor: '#ffffff', // Adds the white gap between slices
      },
    ],
  };

  readonly pieChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: { enabled: true },
    },
  };

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#loadPageOverview();
    this.#loadLicenses();
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

    this.#licenseService.getLicenses().subscribe({
      next: (res) => {
        console.log(res);

        this.licenseTypes.set(res.licenseTypes);
        this.inactiveUsers.set(res.inactiveUsers);
        this.maxLicenseAmount.set(res.maxLicenseAmount);

        this.#updateBarChart();

        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load licenses', err);
        this.isLoading.set(false);
      },
    });
  }

  #loadPageOverview() {}

  #updateBarChart() {
    this.barChartData.labels = this.licenseTypes().map((l) => l.skuName);

    this.barChartData.datasets[0].data = this.licenseTypes().map((l) => l.totalAssigned);
    this.barChartData.datasets[1].data = this.licenseTypes().map((l) => l.totalAvailable);

    if (this.barChartOptions?.scales?.['y']) {
      this.barChartOptions.scales['y'].max = this.maxLicenseAmount();
    }

    this.barChartData = { ...this.barChartData };
  }
}
