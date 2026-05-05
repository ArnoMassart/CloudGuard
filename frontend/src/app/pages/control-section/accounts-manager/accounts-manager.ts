import { Component, inject, OnInit, signal } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcons } from '../../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';
import { FormsModule } from '@angular/forms';
import { AccountSectionType } from '../../../models/account/AccountSectionType';
import { CommonModule } from '@angular/common';
import { AccountsManagerUsers } from './accounts-manager-users/accounts-manager-users';
import { AccountsManagerOrganizations } from './accounts-manager-organizations/accounts-manager-organizations';
import { Router } from '@angular/router';
import { UserService } from '../../../services/user-service';
import { PageContentWrapper } from '../../../components/page-content-wrapper/page-content-wrapper';

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
    PageContentWrapper,
  ],
  templateUrl: './accounts-manager.html',
  styleUrl: './accounts-manager.css',
})
export class AccountsManager implements OnInit {
  readonly Icons = AppIcons;

  readonly #router = inject(Router);
  readonly userService = inject(UserService);

  readonly currentSection = signal<AccountSectionType>('USERS');

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  ngOnInit(): void {
    const section = sessionStorage.getItem('account-section');
    this.loadAccessRequestsCount();
    this.loadDeniedCount();

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

  loadAccessRequestsCount() {
    this.userService.refreshRequestedCount().subscribe({
      next: (res) => {},
      error: (err) => {
        console.error('Error getting the access request count', err);
      },
    });
  }

  loadDeniedCount() {
    this.userService.refreshDeniedCount().subscribe({
      next: (res) => {},
      error: (err) => {
        console.error('Error getting the access request count', err);
      },
    });
  }

  goToRequests() {
    this.#router.navigate(['accounts-manager/requests']);
  }

  goToDenied() {
    this.#router.navigate(['/accounts-manager/denied-list']);
  }
}
