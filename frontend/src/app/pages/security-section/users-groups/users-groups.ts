import { Component, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule, Users } from 'lucide-angular';
import { UsersSection } from './users-section/users-section';
import { GroupsSection } from './groups-section/groups-section';
import { SectionType } from '../../../models/SectionType';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-users-groups',
  imports: [PageHeader, LucideAngularModule, UsersSection, GroupsSection, CommonModule],
  templateUrl: './users-groups.html',
  styleUrl: './users-groups.css',
})
export class UsersGroups {
  readonly users = Users;

  currentSection = signal<SectionType>('USERS');

  togglePage(section: SectionType) {
    if (this.currentSection() !== section) {
      this.currentSection.set(section);
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
