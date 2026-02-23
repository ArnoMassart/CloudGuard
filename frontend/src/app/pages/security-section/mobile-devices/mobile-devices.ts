import { Component, computed, inject, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  LucideAngularModule,
  TriangleAlert,
  Search,
  CircleCheck,
  CircleX,
  Clock,
  ChevronLeft,
  ChevronRight,
  Shield,
  Smartphone,
  ChevronDown,
  ChevronUp,
  ShieldAlert,
  HardDrive,
  Cpu,
  ShieldCheck,
  Lock,
  ExternalLink,
} from 'lucide-angular';
import { UsersSectionTopCard } from '../users-groups/users-section/users-section-top-card/users-section-top-card';
import { MobileDeviceService } from '../../../services/mobile-device-service';
import { MobileDevice } from '../../../models/devices/MobileDevice';
import { MobileDevicesOverviewResponse } from '../../../models/devices/MobileDevicesOverviewResponse';

@Component({
  selector: 'app-mobile-devices',
  imports: [
    PageHeader,
    LucideAngularModule,
    UsersSectionTopCard,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './mobile-devices.html',
  styleUrl: './mobile-devices.css',
})
export class MobileDevices {
  readonly CheckCircle = CircleCheck;
  readonly XCircle = CircleX;
  readonly Clock = Clock;
  readonly TriangleAlert = TriangleAlert;
  readonly ChevronLeft = ChevronLeft;
  readonly ChevronRight = ChevronRight;
  readonly ChevronDown = ChevronDown;
  readonly SmartPhone = Smartphone;
  readonly Shield = Shield;
  readonly ShieldAlert = ShieldAlert;
  readonly Lock = Lock;
  readonly HardDrive = HardDrive;
  readonly Cpu = Cpu;
  readonly ShieldCheck = ShieldCheck;
  readonly ExternalLink = ExternalLink;

  readonly #mobileDeviceService = inject(MobileDeviceService);

  hasWarnings = signal(false);

  devices = signal<MobileDevice[]>([]);

  itemsPerPage: number = 5;

  pageOverview = signal<MobileDevicesOverviewResponse | null>(null);

  // Paging state
  currentPage = signal(1);
  nextPageToken = signal<string | null>(null);
  isLoading = signal(false);

  private tokenHistory: (string | null)[] = [null];

  expandedDevice = signal<string | null>(null);

  uniqueDeviceTypes = signal<string[]>(['Alle device typen']);

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
          this.nextPageToken.set(res.pageToken);
          this.isLoading.set(false);
          console.log(this.devices());
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
    if (this.pageOverview()?.totalNonCompliant! > 0) {
      this.hasWarnings.set(true);
    }
  }

  loadDeviceTypes() {
    this.#mobileDeviceService.getUniqueDeviceTypes().subscribe({
      next: (types) => {
        // Voeg de unieke types uit de backend toe achter de standaard 'Alle' optie
        this.uniqueDeviceTypes.set(['Alle device typen', ...types]);
      },
      error: (err) => {
        console.error('Kon device typen niet laden', err);
      },
    });
  }

  selectedStatus = signal<string>('Alle statussen');
  selectedDeviceType = signal<string>('Alle device typen');

  // Opties voor de status dropdown
  statusOptions = ['Alle statussen', 'Approved', 'Pending', 'Blocked'];

  onStatusChange(newStatus: string) {
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
    const warnings = this.hasWarnings;

    const activeCount = Object.values(warnings).filter((val) => val === true).length;

    return activeCount > 1;
  });
}
