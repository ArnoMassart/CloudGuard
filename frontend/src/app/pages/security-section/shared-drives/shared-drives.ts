import { Component, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { SharedDrivesTopCard } from './shared-drives-top-card/shared-drives-top-card';
import {
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

  hasWarnings = signal(true);
  isLoading = signal(false);

  searchValue = signal('');

  drives = [
    {
      name: 'Projecten 2026',
      risk: 'Middel',
      totalMember: 15,
      externalMembers: 2,
      storageUsed: '45.2 GB',
      lastActivity: '14 dagen geleden',
      shareSettings: 'Extern delen toegestaan',
    },
    {
      name: 'HR Documenten',
      risk: 'Laag',
      totalMember: 8,
      externalMembers: 0,
      storageUsed: '12.8 GB',
      lastActivity: '15 dagen geleden',
      shareSettings: 'Alleen intern',
    },
    {
      name: 'Client Deliverables',
      risk: 'Hoog',
      totalMember: 22,
      externalMembers: 8,
      storageUsed: '89.5 GB',
      lastActivity: '14 dagen geleden',
      shareSettings: 'Extern delen toegestaan',
    },
  ];
}
