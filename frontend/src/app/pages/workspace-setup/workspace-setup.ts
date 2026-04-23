import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { LucideAngularModule } from 'lucide-angular';
import { RouteService } from '../../services/route-service';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LanguageBar } from '../../components/language-bar/language-bar';

@Component({
  selector: 'app-workspace-setup',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule, TranslocoPipe, LanguageBar],
  templateUrl: './workspace-setup.html',
})
export class WorkspaceSetup {
  readonly Icons = AppIcons;

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly authService = inject(CustomAuthService);

  // Status management
  isLoading = signal(false);
  error = signal<string | null>(null);
  copiedField = signal<string | null>(null);

  // Form data
  adminEmail = '';

  // Technische gegevens voor Google Admin Console
  readonly CLIENT_ID = '123456789012345678901'; // Vervang door je echte SA Client ID
  readonly OAUTH_SCOPES = [
    'https://www.googleapis.com/auth/admin.directory.user.readonly',
    'https://www.googleapis.com/auth/admin.directory.device.chromeos.readonly',
    'https://www.googleapis.com/auth/admin.directory.device.mobile.readonly',
  ].join(', ');

  readonly #authService = inject(CustomAuthService);

  tryAgain() {
    this.#authService.logout();
  }

  copyToClipboard(text: string, field: string) {
    navigator.clipboard.writeText(text);
    this.copiedField.set(field);
    setTimeout(() => this.copiedField.set(null), 2000);
  }

  onSubmit() {
    if (!this.adminEmail) return;

    this.isLoading.set(true);
    this.error.set(null);

    const payload = { adminEmail: this.adminEmail }; // [cite: 14]
    const url = RouteService.getBackendUrl('/api/workspace/setup');

    this.http.post(url, payload, { withCredentials: true }).subscribe({
      next: () => {
        // Herlaad de sessie om de nieuwe SUPER_ADMIN rol op te halen [cite: 18]
        this.authService.checkServerSession().subscribe(() => {
          this.router.navigate(['/dashboard']); // [cite: 24]
        });
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set('setup.error_verification_failed'); // Vertaling voor falende DWD test [cite: 17]
        console.error('Setup error:', err);
      },
    });
  }
}
