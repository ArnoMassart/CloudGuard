import { Component, inject, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { UserService } from '../../services/user-service';
import { CustomAuthService } from '../../auth/custom-auth-service';
import { take } from 'rxjs';

@Component({
  selector: 'app-language-bar',
  imports: [CommonModule, FormsModule],
  templateUrl: './language-bar.html',
  styleUrl: './language-bar.css',
})
export class LanguageBar implements OnInit {
  readonly #userService = inject(UserService);
  readonly #authService = inject(CustomAuthService);
  readonly #translocoService = inject(TranslocoService);

  activeLang: string = this.#translocoService.getActiveLang();

  languages = [
    { label: 'Nederlands', value: 'nl', flag: '/img/nl.png' },
    { label: 'English', value: 'en', flag: '/img/en.png' },
  ];

  ngOnInit(): void {
    const langFromStorage = localStorage.getItem('currentLang');

    if (langFromStorage) {
      this.#updateState(langFromStorage);
    } else {
      // Wacht tot de gebruiker is ingelogd voordat we de backend lastigvallen
      this.#authService.isLoggedIn$.pipe(take(2)).subscribe((isLoggedIn) => {
        if (isLoggedIn) {
          this.#loadLangFromBackend();
        }
      });
    }
  }

  #loadLangFromBackend() {
    this.#userService.getLanguage().subscribe({
      next: (lang) => {
        if (lang) {
          this.#updateState(lang);
          localStorage.setItem('currentLang', lang);
        }
      },
      error: (err) => console.error('Failed to load language', err),
    });
  }

  #updateState(lang: string) {
    this.activeLang = lang;
    this.#translocoService.setActiveLang(lang);
  }

  getSelectedFlag(): string {
    const selected = this.languages.find((lang) => lang.value === this.activeLang);
    return selected ? selected.flag : '';
  }

  changeLanguage(newLang: string) {
    this.#updateState(newLang);
    localStorage.setItem('currentLang', newLang);

    // Alleen opslaan in DB als we daadwerkelijk een ingelogde gebruiker hebben
    this.#authService.isLoggedIn$.pipe(take(1)).subscribe((isLoggedIn) => {
      if (isLoggedIn) {
        this.#userService.updateLanguage(newLang).subscribe({
          next: () => console.log('Taal opgeslagen in DB'),
          error: (err) => console.error('DB save error', err),
        });
      }
    });
  }
}
