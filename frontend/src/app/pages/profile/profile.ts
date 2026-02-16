import { Component, inject, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-profile',
  imports: [CommonModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  private auth = inject(AuthService);

  readonly user$ = this.auth.user$;
  closed$ = output<void>();

  close(){
    this.closed$.emit();
  }

  logout(){
    this.auth.logout();
  }

  getInitials(user: { name?: string; given_name?: string; family_name?: string; email?: string }) {
    if (user?.given_name && user?.family_name)
      return (user.given_name[0] + user.family_name[0]).toUpperCase();
    if (user?.name) {
      const parts = user.name.trim().split(/\s+/);
      return parts.length >= 2
        ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
        : user.name.slice(0, 2).toUpperCase();
    }
    if (user?.email) return user.email.slice(0, 2).toUpperCase();
    return '?';
  }
}
