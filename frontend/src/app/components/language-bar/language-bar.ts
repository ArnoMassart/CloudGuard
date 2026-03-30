import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { UserService } from '../../services/user-service';

@Component({
  selector: 'app-language-bar',
  imports: [CommonModule, FormsModule],
  templateUrl: './language-bar.html',
  styleUrl: './language-bar.css',
})
export class LanguageBar {
  readonly #userService = inject(UserService);

  activeLang: string;

  languages = [
    { label: 'Nederlands', value: 'nl', flag: '/img/nl.png' },
    { label: 'English', value: 'en', flag: '/img/en.png' },
  ];

  getSelectedFlag(): string {
    const selected = this.languages.find((lang) => lang.value === this.activeLang);
    return selected ? selected.flag : '';
  }

  constructor(private translocoService: TranslocoService) {
    const langFromStorage = localStorage.getItem('currentLang');

    if (langFromStorage !== null) {
      // 1. Language found in local storage. Use it immediately.
      this.activeLang = langFromStorage;
      this.translocoService.setActiveLang(langFromStorage);
    } else {
      // 2. Set a temporary fallback language so the UI doesn't break while waiting for the backend
      this.activeLang = this.translocoService.getActiveLang();

      // 3. Fetch the actual preference from the backend
      this.#userService.getLanguage().subscribe({
        next: (lang) => {
          // 4. Update the state and local storage once the backend responds
          this.activeLang = lang;
          this.translocoService.setActiveLang(lang);
          localStorage.setItem('currentLang', lang);
        },
        error: (err) => {
          // 5. Optional: Handle the error gracefully
          console.error('Failed to load language from backend', err);
        },
      });
    }
  }

  changeLanguage(newLang: string) {
    localStorage.setItem('currentLang', newLang);

    this.activeLang = newLang;
    this.translocoService.setActiveLang(newLang);

    this.#userService.updateLanguage(newLang).subscribe({
      next: () => console.log('Taal successvol opgeslagen'),
      error: (err) => console.error('Error by saving language in database', err),
    });
  }
}
