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
import { AppIcons } from '../../../shared/app-icons';

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
  readonly #mobileDeviceService = inject(MobileDeviceService);

  readonly Icons = AppIcons;

  readonly statusEnum = MobileDeviceStatus;

  hasWarnings = signal(false);
  devicePageWarnings = signal<MobileDevicesPageWarnings>({
    lockScreenWarning: false,
    encryptionWarning: false,
    osVersionWarning: false,
    integrityWarning: false,
  });

  readonly isExpanded = signal(true);
  MobileDeviceStatus: any;

  toggleExpanded() {
    this.isExpanded.update((v) => !v);
  }

  devices = signal<MobileDevice[]>([]);

  itemsPerPage: number = 4;

  pageOverview = signal<MobileDevicesOverviewResponse | null>(null);

  // Paging state
  currentPage = signal(1);
  nextPageToken = signal<string | null>(null);
  isLoading = signal(false);

  private tokenHistory: (string | null)[] = [null];

  expandedDevice = signal<string | null>(null);

  uniqueDeviceTypes = signal<string[]>(['Alle apparaat typen']);

  selectedDeviceType = signal<string>('Alle apparaat typen');

  selectedStatus = signal<MobileDeviceStatus>(MobileDeviceStatus.All);
  statusOptions = Object.values(MobileDeviceStatus);

  toggleExpand(deviceId: string) {
    if (this.expandedDevice() === deviceId) {
      this.expandedDevice.set(null);
    } else {
      this.expandedDevice.set(deviceId);
    }
  }

  ngOnInit(): void {
    this.loadDeviceTypes();
    this.loadPageOverview();
    this.loadMobileDevices();
  }

  loadMobileDevices(token: string | null = null) {
    this.isLoading.set(true);

    this.#mobileDeviceService
      .getDevices(
        token || undefined,
        this.selectedStatus(),
        this.selectedDeviceType(),
        this.itemsPerPage
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

  loadPageOverview() {
    this.#mobileDeviceService.getMobileDevicesPageOverview().subscribe({
      next: (res) => {
        this.pageOverview.set(res);
        this.loadWarnings();
      },
      error: (err) => {
        console.error('Failed to load page overview', err);
      },
    });
  }

  loadWarnings() {
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

  loadDeviceTypes() {
    this.#mobileDeviceService.getUniqueDeviceTypes().subscribe({
      next: (types) => {
        // Voeg de unieke types uit de backend toe achter de standaard 'Alle' optie
        this.uniqueDeviceTypes.set(['Alle apparaat typen', ...types]);
      },
      error: (err) => {
        console.error('Kon apparaat typen niet laden', err);
      },
    });
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
      this.tokenHistory.push(token); // Onthoud dit token om terug te kunnen
      this.currentPage.update((p) => p + 1);
      this.loadMobileDevices(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.tokenHistory.pop(); // Verwijder huidige token
      const prevToken = this.tokenHistory[this.tokenHistory.length - 1]; // Pak de vorige
      this.currentPage.update((p) => p - 1);
      this.loadMobileDevices(prevToken);
    }
  }

  #resetAndLoad() {
    this.currentPage.set(1);
    this.tokenHistory = [null]; // Reset de paginatie-historie
    this.expandedDevice.set(null); // Sluit eventueel opengeklapte rijen
    this.loadMobileDevices(null);
  }

  openAdminPage() {
    window.open(`https://admin.google.com/ac/devices/list?default=true&category=mobile`);
  }

  hasMultipleWarnings = computed(() => {
    const warnings = this.devicePageWarnings();

    const activeCount = Object.values(warnings).filter((val) => val === true).length;

    return activeCount > 1;
  });
}
