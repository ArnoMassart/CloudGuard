import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LucideAngularModule } from 'lucide-angular';
import { DeviceService } from '../../../services/device-service';
import { Device } from '../../../models/devices/Device';
import { DevicesOverviewResponse } from '../../../models/devices/DevicesOverviewResponse';
import { DevicesPageWarnings } from '../../../models/devices/DevicesPageWarnings';
import { DeviceStatus } from '../../../models/devices/DeviceStatus';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 5;

@Component({
  selector: 'app-devices',
  imports: [
    PageHeader,
    LucideAngularModule,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
    SectionTopCard,
    PageWarnings,
    PageWarningsItem,
  ],
  templateUrl: './devices.html',
  styleUrl: './devices.css',
})
export class Devices implements OnInit {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly statusEnum = DeviceStatus;
  readonly UtilityMethods = UtilityMethods;
  readonly #deviceService = inject(DeviceService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);

  readonly devices = signal<Device[]>([]);
  readonly pageOverview = signal<DevicesOverviewResponse | null>(null);
  readonly expandedDevice = signal<string | null>(null);

  readonly uniqueDeviceTypes = signal<string[]>(['Alle apparaat typen']);
  readonly selectedDeviceType = signal<string>('Alle apparaat typen');

  readonly selectedStatus = signal<DeviceStatus>(DeviceStatus.All);
  readonly statusOptions = Object.values(DeviceStatus);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasWarnings = signal(false);
  readonly devicePageWarnings = signal<DevicesPageWarnings>({
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
    this.#loadDevices();
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

  onStatusChange(newStatus: DeviceStatus) {
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
      this.#loadDevices(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1);
      this.currentPage.update((p) => p - 1);
      this.#loadDevices(prevToken);
    }
  }

  refreshData() {
    if (this.isRefreshing()) return;

    this.isRefreshing.set(true);

    this.#deviceService.refreshDeviceCache().subscribe({
      next: (res) => {
        this.currentPage.set(1);
        this.#tokenHistory = [null];

        this.#loadDevices(null);
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
  #loadDevices(token: string | null = null) {
    this.isLoading.set(true);

    this.#deviceService
      .getDevices(
        token || undefined,
        this.selectedStatus(),
        this.selectedDeviceType(),
        ITEMS_PER_PAGE,
      )
      .subscribe({
        next: (res) => {
          this.devices.set(res.devices);
          this.nextPageToken.set(res.nextPageToken);
          this.isLoading.set(false);
        },
        error: (err) => {
          console.error('Failed to load devices', err);
          this.isLoading.set(false);
        },
      });
  }

  #loadPageOverview() {
    this.#deviceService.getDevicesPageOverview().subscribe({
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
    this.#deviceService.getUniqueDeviceTypes().subscribe({
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
    this.#loadDevices(null);
  }
}
