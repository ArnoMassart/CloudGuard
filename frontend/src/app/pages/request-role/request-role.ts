import { Component, inject, OnInit, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { UserService } from '../../services/user-service';
import { AppIcons } from '../../shared/AppIcons';
import { StatusLayout } from '../../components/status-layout/status-layout';
import { StatusRequestCard } from '../../components/status-request-card/status-request-card';

@Component({
  selector: 'app-request-role',
  imports: [LucideAngularModule, StatusLayout, StatusRequestCard],
  templateUrl: './request-role.html',
  styleUrl: './request-role.css',
})
export class RequestRole implements OnInit {
  readonly Icons = AppIcons;

  readonly #userService = inject(UserService);

  requestSent = signal(false);

  ngOnInit(): void {
    this.getRequestSent();
  }

  requestRole() {
    this.#userService.requestAccess('/request-role').subscribe({
      next: () => {
        this.requestSent.set(true);
      },
      error: (err) => console.error('Fout bij aanvraag', err),
    });
  }

  getRequestSent() {
    this.#userService.getRequestSent('/request-role').subscribe({
      next: (val) => {
        this.requestSent.set(val);
      },
      error: (err) => console.error('Fout bij ophalen request', err),
    });
  }
}
