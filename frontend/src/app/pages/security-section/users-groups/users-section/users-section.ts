import { Component, signal } from '@angular/core';
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

@Component({
  selector: 'app-users-section',
  imports: [LucideAngularModule, UsersSectionTopCard, FormsModule, CommonModule],
  templateUrl: './users-section.html',
  styleUrl: './users-section.css',
})
export class UsersSection {
  readonly triangleAlertIcon = TriangleAlert;
  readonly searchIcon = Search;
  readonly checkCircle = CircleCheck;
  readonly xCircle = CircleX;
  readonly clock = Clock;
  readonly triangleAlert = TriangleAlert;
  readonly chevronLeft = ChevronLeft;
  readonly chevronRight = ChevronRight;

  hasWarnings = signal(true);
  searchParam: string = '';

  currentPage: number = 1;
  itemsPerPage: number = 10; // Adjust based on your design

  get totalPages(): number {
    return Math.ceil(this.users.length / this.itemsPerPage);
  }

  get paginatedUsers() {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return this.users.slice(startIndex, startIndex + this.itemsPerPage);
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

  users = [
    {
      name: 'Jan Janssen',
      email: 'jan.janssen@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
    {
      name: 'Maria van der Berg',
      email: 'jan.janssen@be5drijf.nl',
      role: 'Security Admin',
      status: 'Suspended',
      lastLogin: '2 maanden geleden',
      twoFA: false,
      activity: 'Verdacht',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.jafdhden@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.janssen@bedrijf.nl5',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.janssen@bedrij5f.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'ja5n.janssen@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: false,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.jan5ssen@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.j5anssen@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: false,
      activity: 'Verdacht',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.janssen@5bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: false,
      activity: 'Normaal',
    },
    {
      name: 'Jan Janssen',
      email: 'jan.janssenf@bedrijf.nl',
      role: 'Super Admin',
      status: 'Actief',
      lastLogin: '12 dagen geleden',
      twoFA: true,
      activity: 'Normaal',
    },
  ];
}
