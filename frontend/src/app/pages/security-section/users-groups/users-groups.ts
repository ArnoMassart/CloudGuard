import { Component, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';
import { UsersSection } from './users-section/users-section';
import { GroupsSection } from './groups-section/groups-section';
import { SectionType } from '../../../models/SectionType';
import { CommonModule } from '@angular/common';
import { AppIcons } from '../../../shared/AppIcons';

@Component({
  selector: 'app-users-groups',
  imports: [PageHeader, LucideAngularModule, UsersSection, GroupsSection, CommonModule],
  templateUrl: './users-groups.html',
  styleUrl: './users-groups.css',
})
export class UsersGroups implements OnInit {
  // ==========================================
  // INJECTIONS
  // ==========================================
  readonly Icons = AppIcons;

  // ==========================================
  // PUBLIC PROPERTIES & SIGNALS
  // ==========================================
  currentSection = signal<SectionType>('USERS');

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    const section = sessionStorage.getItem('user-group-section');

    if (section === 'USERS' || section === 'GROUPS') {
      this.currentSection.set(section);
    } else {
      this.currentSection.set('USERS');
    }
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================
  togglePage(section: SectionType) {
    if (this.currentSection() !== section) {
      this.currentSection.set(section);
      sessionStorage.setItem('user-group-section', section);
    }
  }

  getTabClass(section: SectionType) {
    const isActive = this.currentSection() === section;
    return {
      'border-primary text-black': isActive,
      'border-transparent text-slate-500 hover:text-slate-700 cursor-pointer': !isActive,
      'flex items-center gap-2 pb-3 border-b-2 -mb-[1px] transition-all': true,
    };
  }
}
