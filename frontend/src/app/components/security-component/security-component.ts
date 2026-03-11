import { Component, EventEmitter, inject, Input } from '@angular/core';
import { AppIcons } from '../../shared/AppIcons';
import { LucideAngularModule, LucideIconData } from 'lucide-angular';
import { Router } from '@angular/router';

@Component({
  selector: 'app-security-component',
  imports: [LucideAngularModule],
  templateUrl: './security-component.html',
  styleUrl: './security-component.css',
})
export class SecurityComponent {
  readonly Icons = AppIcons;
  readonly #router = inject(Router);

  @Input() Label: string = 'Gebruikers';
  @Input() Value: number | undefined = 68;
  @Input() Icon: LucideIconData = this.Icons.Users;
  @Input() NoValueShow: boolean = false;
  @Input() Route: string = '/users-groups';
  @Input() IsGroups: boolean = false;

  handleClick() {
    if (this.Route === '/users-groups') {
      if (this.IsGroups) {
        sessionStorage.setItem('user-group-section', 'GROUPS');
      } else {
        sessionStorage.setItem('user-group-section', 'USERS');
      }
    }

    this.#router.navigate([this.Route]);
  }
}
