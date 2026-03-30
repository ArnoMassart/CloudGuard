import { Component, computed, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LucideAngularModule } from 'lucide-angular';
import { DeviceService } from '../../../services/device-service';
import { Device, DeviceFactor } from '../../../models/devices/Device';
import { DevicesOverviewResponse } from '../../../models/devices/DevicesOverviewResponse';
import { DevicesPageWarnings } from '../../../models/devices/DevicesPageWarnings';
import { DeviceStatus } from '../../../models/devices/DeviceStatus';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { UtilityMethods } from '../../../shared/UtilityMethods';
import { PageWarnings } from '../../../components/page-warnings/page-warnings';
import { PageWarningsItem } from '../../../components/page-warnings/page-warnings-item/page-warnings-item';
import { SecurityScoreDetailService } from '../../../services/security-score-detail.service';
import { SecurityPreferencesFacade } from '../../../services/security-preferences-facade';
import { forkJoin } from 'rxjs';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { Subscription } from 'rxjs';
import { PageContentWrapper } from '../../../components/page-content-wrapper/page-content-wrapper';

// ==========================================
// CONSTANTS
// ==========================================
const ITEMS_PER_PAGE = 4;

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
    TranslocoPipe,
    PageContentWrapper,
  ],
  templateUrl: './devices.html',
  styleUrl: './devices.css',
})
export class Devices implements OnInit, OnDestroy {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;
  readonly statusEnum = DeviceStatus;
  readonly UtilityMethods = UtilityMethods;
  readonly #deviceService = inject(DeviceService);
  readonly #securityScoreDetail = inject(SecurityScoreDetailService);
  readonly #preferencesFacade = inject(SecurityPreferencesFacade);

  readonly #translocoService = inject(TranslocoService);

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  readonly isExpanded = signal(true);
  readonly apiError = signal(false);

  readonly devices = signal<Device[]>([]);
  readonly pageOverview = signal<DevicesOverviewResponse | null>(null);
  readonly expandedDevice = signal<string | null>(null);

  readonly uniqueDeviceTypes = signal<string[]>(['all']);
  readonly selectedDeviceType = signal<string>('all');

  readonly selectedStatus = signal<DeviceStatus>(DeviceStatus.All);
  readonly statusOptions = Object.values(DeviceStatus);

  readonly currentPage = signal(1);
  readonly nextPageToken = signal<string | null>(null);
  readonly isLoading = signal(false);
  readonly isRefreshing = signal<boolean>(false);

  readonly hasWarnings = computed(() => this.pageOverview()?.warnings?.hasWarnings ?? false);
  readonly hasMultipleWarnings = computed(
    () => this.pageOverview()?.warnings?.hasMultipleWarnings ?? false
  );
  readonly devicePageWarnings = computed((): DevicesPageWarnings => {
    const items = this.pageOverview()?.warnings?.items ?? {};
    return {
      lockScreenWarning: items['lockScreenWarning'] ?? false,
      encryptionWarning: items['encryptionWarning'] ?? false,
      osVersionWarning: items['osVersionWarning'] ?? false,
      integrityWarning: items['integrityWarning'] ?? false,
    };
  });

  // ==========================================
  // PRIVATE PROPERTIES
  // ==========================================
  #tokenHistory: (string | null)[] = [null];
  #langSubscription?: Subscription;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    this.#loadDeviceTypes();
    this.#langSubscription = this.#translocoService.langChanges$.subscribe(() => {
      this.#loadPageOverview();
      this.#loadDevices();
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

  openSecurityScoreDetail() {
    const overview = this.pageOverview();
    const breakdown =
      overview?.securityScoreBreakdown ??
      this.#securityScoreDetail.createSimpleBreakdown(overview?.securityScore ?? 0, 'devices');
    this.#securityScoreDetail.open(breakdown, 'devices');
  }

  getDeviceFactors(device: Device): DeviceFactor[] {
    return [
      {
        key: 'lockscreen',
        label: 'Vergrendelscherm',
        icon: AppIcons.Lock,
        secure: device.lockSecure,
        text: device.screenLockText,
      },
      {
        key: 'encryption',
        label: 'Encryptie',
        icon: AppIcons.HardDrive,
        secure: device.encSecure,
        text: device.encryptionText,
      },
      {
        key: 'osVersion',
        label: 'OS Versie',
        icon: AppIcons.Cpu,
        secure: device.osSecure,
        text: device.osText,
      },
      {
        key: 'integrity',
        label: 'Integriteit',
        icon: AppIcons.ShieldCheck,
        secure: device.intSecure,
        text: device.integrityText,
      },
    ].map((f) => ({
      ...f,
      state: f.secure
        ? ('ok' as const)
        : this.#preferencesFacade.isDisabled('mobile-devices', f.key)
        ? ('muted' as const)
        : ('warn' as const),
    }));
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
    this.apiError.set(false);

    this.#deviceService
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
          console.error('Failed to load devices', err);
          this.isLoading.set(false);
          this.apiError.set(true);
        },
      });
  }

  #loadPageOverview() {
    this.#preferencesFacade.loadWithPrefs$(this.#deviceService.getDevicesPageOverview()).subscribe({
      next: (overview) => this.pageOverview.set(overview),
      error: (err) => console.error('Failed to load page overview', err),
    });
  }

  #loadDeviceTypes() {
    this.#deviceService.getUniqueDeviceTypes().subscribe({
      next: (types) => {
        this.uniqueDeviceTypes.set(['all', ...types]);
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
