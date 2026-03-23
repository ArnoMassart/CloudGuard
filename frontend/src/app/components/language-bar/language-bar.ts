import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

@Component({
  selector: 'app-language-bar',
  imports: [CommonModule, FormsModule],
  templateUrl: './language-bar.html',
  styleUrl: './language-bar.css',
})
export class LanguageBar {
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
      this.activeLang = langFromStorage;

      this.translocoService.setActiveLang(langFromStorage);
    } else {
      this.activeLang = this.translocoService.getActiveLang();
    }
  }

  changeLanguage(newLang: string) {
    localStorage.setItem('currentLang', newLang);

    this.activeLang = newLang;
    this.translocoService.setActiveLang(newLang);
  }
}
