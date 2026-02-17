import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  getInitials(user: { firstName?: string; lastName?: string; email?: string }) {
    if (user?.firstName && user?.lastName)
      return (user.firstName[0] + user.lastName[0]).toUpperCase();
    if (user?.firstName) return user.firstName.slice(0, 2).toUpperCase();
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }

  getRole(user: { roles: string[] }): string {
    return user.roles.length > 0 ? user.roles[0] : 'Admin';
  }
}
