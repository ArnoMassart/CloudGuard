import { Component, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { AccountSectionType } from '../../../models/AccountSectionType';
import { CommonModule } from '@angular/common';
import { AccountsManagerUsers } from './accounts-manager-users/accounts-manager-users';
import { AccountsManagerOrganizations } from './accounts-manager-organizations/accounts-manager-organizations';

@Component({
  selector: 'app-accounts-manager',
  imports: [
    PageHeader,
    TranslocoPipe,
    LucideAngularModule,
    FormsModule,
    CommonModule,
    AccountsManagerUsers,
    AccountsManagerOrganizations,
  ],
  templateUrl: './accounts-manager.html',
  styleUrl: './accounts-manager.css',
})
export class AccountsManager implements OnInit {
  readonly Icons = AppIcons;

  readonly currentSection = signal<AccountSectionType>('USERS');

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    const section = sessionStorage.getItem('account-section');

    if (section === 'USERS' || section === 'ORGANIZATIONS') {
      this.currentSection.set(section);
    } else {
      this.currentSection.set('USERS');
    }
  }

  // ==========================================
  // PUBLIC METHODS
  // ==========================================

  togglePage(section: AccountSectionType) {
    if (this.currentSection() !== section) {
      this.currentSection.set(section);
      sessionStorage.setItem('account-section', section);
    }
  }

  getTabClass(section: AccountSectionType) {
    const isActive = this.currentSection() === section;
    return {
      'border-primary text-black': isActive,
      'border-transparent text-slate-500 hover:text-slate-700 cursor-pointer': !isActive,
      'flex items-center gap-2 pb-3 border-b-2 -mb-[1px] transition-all': true,
    };
  }
}
