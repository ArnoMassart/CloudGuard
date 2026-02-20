import { Component, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { SharedDrivesTopCard } from './shared-drives-top-card/shared-drives-top-card';
import {
  CircleCheck,
  Clock,
  ExternalLink,
  FolderOpen,
  HardDrive,
  LucideAngularModule,
  Search,
  TriangleAlert,
  Users,
  Users2,
} from 'lucide-angular';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-shared-drives',
  imports: [PageHeader, SharedDrivesTopCard, LucideAngularModule, FormsModule],
  templateUrl: './shared-drives.html',
  styleUrl: './shared-drives.css',
})
export class SharedDrives {
  readonly FolderOpen = FolderOpen;
  readonly HardDrive = HardDrive;
  readonly TriangleAlert = TriangleAlert;
  readonly Users = Users;
  readonly Search = Search;
  readonly Clock = Clock;
  readonly ExternalLink = ExternalLink;
  readonly CircleCheck = CircleCheck;

  hasWarnings = signal(true);
  isLoading = signal(false);

  searchValue = signal('');

  drives = [
    {
      name: 'Projecten 2026',
      risk: 'Hoog',
      totalMember: 15,
      externalMembers: 2,
      storageUsed: '45.2 GB',
      lastActivity: '14 dagen geleden',
      shareSettingsLabel: 'Extern delen toegestaan',
      shareExternalAllowed: true,
    },
    {
      name: 'HR Documenten',
      risk: 'Laag',
      totalMember: 8,
      externalMembers: 0,
      storageUsed: '12.8 GB',
      lastActivity: '15 dagen geleden',
      shareSettingsLabel: 'Alleen intern',
      shareExternalAllowed: false,
    },
    {
      name: 'Client Deliverables',
      risk: 'Hoog',
      totalMember: 22,
      externalMembers: 8,
      storageUsed: '89.5 GB',
      lastActivity: '14 dagen geleden',
      shareSettingsLabel: 'Extern delen toegestaan',
      shareExternalAllowed: true,
    },
    {
      name: 'Client Deliverables',
      risk: 'Middel',
      totalMember: 22,
      externalMembers: 0,
      storageUsed: '89.5 GB',
      lastActivity: '14 dagen geleden',
      shareSettingsLabel: 'Extern delen toegestaan',
      shareExternalAllowed: true,
    },
  ];

  openAdminPage() {
    window.open(`https://admin.google.com/ac/drive/manageshareddrives`);
  }
}
