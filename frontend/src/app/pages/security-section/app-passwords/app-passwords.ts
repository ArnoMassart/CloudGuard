import { Component, inject, OnInit, signal } from '@angular/core';
import { AppPasswordsService } from '../../../services/app-password-service';
import { CommonModule } from '@angular/common';
import { SectionTopCard } from '../../../components/section-top-card/section-top-card';
import { AppIcons } from '../../../shared/AppIcons';
import { App } from '../../../app';
import { PageHeader } from '../../../components/page-header/page-header';
import { AppPassword } from '../../../services/app-password-service';
import {AppPasswordOverviewResponse} from "../../../services/app-password-service";

@Component({
  selector: 'app-app-passwords',
  imports: [SectionTopCard, CommonModule, PageHeader],
  templateUrl: './app-passwords.html',
  styleUrl: './app-passwords.css',
})
export class AppPasswords implements OnInit{
  readonly Icons = AppIcons;
  readonly pageOverview = signal<AppPasswordOverviewResponse|null>(null);
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
