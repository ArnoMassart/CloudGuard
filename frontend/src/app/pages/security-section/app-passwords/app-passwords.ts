import { Component, inject, OnInit, signal } from '@angular/core';
import { AppPasswordsService } from '../../../services/app-password-service';

export interface AppPassword {
  userEmail: string;
  codeId: number;
  name: string;
  createdAt: Date;
  lastUsedAt: Date | null;
}

@Component({
  selector: 'app-app-passwords',
  imports: [],
  templateUrl: './app-passwords.html',
  styleUrl: './app-passwords.css',
})
export class AppPasswords implements OnInit{
  readonly #appPasswordsService = inject(AppPasswordsService);
  readonly appPasswords = signal<AppPassword[]>([]);

  ngOnInit(): void{
    this.#loadAppPasswords();
  }

  #loadAppPasswords() {
    this.#appPasswordsService.getAppPasswords().subscribe({
      next: (passwords) => this.appPasswords.set(passwords),
      error: (err) => console.error('Failed to load app passwords', err),
    });
  }
}
