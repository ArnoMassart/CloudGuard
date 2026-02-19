import { Component, inject, OnInit, signal } from '@angular/core';
import {
  ChevronLeft,
  ChevronRight,
  CircleCheck,
  CircleX,
  Clock,
  LucideAngularModule,
  Search,
  TriangleAlert,
} from 'lucide-angular';
import { UsersSectionTopCard } from './users-section-top-card/users-section-top-card';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { UserOrgDetail } from '../../../../models/UserOrgDetails';
import { UserService } from '../../../../services/user-service';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-users-section',
  imports: [
    LucideAngularModule,
    UsersSectionTopCard,
    FormsModule,
    CommonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './users-section.html',
  styleUrl: './users-section.css',
})
export class UsersSection implements OnInit {
  readonly triangleAlertIcon = TriangleAlert;
  readonly searchIcon = Search;
  readonly checkCircle = CircleCheck;
  readonly xCircle = CircleX;
  readonly clock = Clock;
  readonly triangleAlert = TriangleAlert;
  readonly chevronLeft = ChevronLeft;
  readonly chevronRight = ChevronRight;

  readonly #userService = inject(UserService);

  hasWarnings = signal(true);
  searchParam: string = '';

  currentPage: number = 1;
  itemsPerPage: number = 10; // Adjust based on your design

  orgUsers = signal<UserOrgDetail[]>([]);

  ngOnInit(): void {
    this.getAllUsers();
  }

  getAllUsers() {
    this.#userService.getOrgUsers().subscribe({
      next: (data) => {
        this.orgUsers.set(data);
        console.log(this.orgUsers());
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.orgUsers.set([]);
      },
    });
  }

  get totalPages(): number {
    const pages = Math.ceil(this.orgUsers().length / this.itemsPerPage);
    return pages;
  }

  get paginatedUsers() {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    if (this.orgUsers().length >= 0) {
      return this.orgUsers().slice(startIndex, startIndex + this.itemsPerPage);
    }

    return [];
  }

  setPage(page: number) {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  searchValueChanged(value: string) {
    this.searchParam = value;
  }

  getRoleClass(role: string) {
    switch (role) {
      case 'Super Admin':
        return 'bg-primary text-white';
      case 'Security Admin':
        return 'bg-purple-100 text-purple-700';
      case 'Regular User':
        return 'bg-blue-100 text-blue-700';
      case 'User Admin':
        return 'bg-fuchsia-100 text-fuchsia-700';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }
}
