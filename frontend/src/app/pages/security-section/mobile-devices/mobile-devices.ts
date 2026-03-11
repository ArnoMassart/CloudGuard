import { Component, computed, inject, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LucideAngularModule } from 'lucide-angular';
import { MobileDeviceService } from '../../../services/mobile-device-service';
import { MobileDevice } from '../../../models/devices/MobileDevice';
import { MobileDevicesOverviewResponse } from '../../../models/devices/MobileDevicesOverviewResponse';
import { MobileDevicesPageWarnings } from '../../../models/devices/MobileDevicesPageWarnings';
import { MobileDeviceStatus } from '../../../models/devices/MobileDeviceStatus';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { delay } from 'rxjs';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 4;

@Component({
  selector: 'app-mobile-devices',
  imports: [
    PageHeader,
    LucideAngularModule,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
    SectionTopCard,
  ],
  templateUrl: './mobile-devices.html',
  styleUrl: './mobile-devices.css',
})
export class MobileDevices {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly statusEnum = MobileDeviceStatus;
  readonly UtilityMethods = UtilityMethods;
  readonly #mobileDeviceService = inject(MobileDeviceService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  readonly devices = signal<MobileDevice[]>([]);
  readonly pageOverview = signal<MobileDevicesOverviewResponse | null>(null);
  readonly expandedDevice = signal<string | null>(null);

  readonly uniqueDeviceTypes = signal<string[]>(['Alle apparaat typen']);
  readonly selectedDeviceType = signal<string>('Alle apparaat typen');

  readonly selectedStatus = signal<MobileDeviceStatus>(MobileDeviceStatus.All);
  readonly statusOptions = Object.values(MobileDeviceStatus);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasWarnings = signal(false);
  readonly devicePageWarnings = signal<MobileDevicesPageWarnings>({
    lockScreenWarning: false,
    encryptionWarning: false,
    osVersionWarning: false,
    integrityWarning: false,
  });

  readonly hasMultipleWarnings = computed(() => {
    const warnings = this.devicePageWarnings();
    const activeCount = Object.values(warnings).filter((val) => val === true).length;
    return activeCount > 1;
  });

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#loadDeviceTypes();
    this.#loadPageOverview();
    this.#loadMobileDevices();
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  toggleExpand(deviceId: string) {
    if (this.expandedDevice() === deviceId) {
      this.expandedDevice.set(null);
    } else {
      this.expandedDevice.set(deviceId);
    }
  }

  onStatusChange(newStatus: MobileDeviceStatus) {
    this.selectedStatus.set(newStatus);
    this.#resetAndLoad();
  }

  onDeviceTypeChange(newType: string) {
    this.selectedDeviceType.set(newType);
    this.#resetAndLoad();
  }

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.#loadMobileDevices(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory[this.#tokenHistory.length - 1];
      this.currentPage.update((p) => p - 1);
      this.#loadMobileDevices(prevToken);
    }
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#mobileDeviceService.refreshDeviceCache().subscribe({
      next: (res) => {
        this.currentPage.set(1);
        this.#tokenHistory = [null];

        this.#loadMobileDevices(null);
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
  #loadMobileDevices(token: string | null = null) {
    this.isLoading.set(true);

    this.#mobileDeviceService
      .getDevices(
        token || undefined,
        this.selectedStatus(),
        this.selectedDeviceType(),
        ITEMS_PER_PAGE
      )
      .subscribe({
        next: (res) => {
          this.devices.set(res.devices);
          this.nextPageToken.set(res.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load mobile devices', err);
          this.isLoading.set(false);
        },
      });
  }

  #loadPageOverview() {
    this.#mobileDeviceService.getMobileDevicesPageOverview().subscribe({
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
    if (this.pageOverview()?.lockScreenCount! > 0) {
      this.hasWarnings.set(true);
      this.devicePageWarnings().lockScreenWarning = true;
    }

    if (this.pageOverview()?.encryptionCount! > 0) {
      this.hasWarnings.set(true);
      this.devicePageWarnings().encryptionWarning = true;
    }

    if (this.pageOverview()?.osVersionCount! > 0) {
      this.hasWarnings.set(true);
      this.devicePageWarnings().osVersionWarning = true;
    }

    if (this.pageOverview()?.integrityCount! > 0) {
      this.hasWarnings.set(true);
      this.devicePageWarnings().integrityWarning = true;
    }
  }

  #loadDeviceTypes() {
    this.#mobileDeviceService.getUniqueDeviceTypes().subscribe({
      next: (types) => {
        this.uniqueDeviceTypes.set(['Alle apparaat typen', ...types]);
      },
      error: (err) => {
        console.error('Kon apparaat typen niet laden', err);
      },
    });
  }

  #resetAndLoad() {
    this.currentPage.set(1);
    this.#tokenHistory = [null];
    this.expandedDevice.set(null);
    this.#loadMobileDevices(null);
  }
}
