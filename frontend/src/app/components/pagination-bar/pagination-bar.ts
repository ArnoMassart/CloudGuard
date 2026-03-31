import { Component, input, Input, output, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-pagination-bar',
  standalone: true,
  imports: [TranslocoPipe, LucideAngularModule],
  templateUrl: './pagination-bar.html',
  styleUrl: './pagination-bar.css',
})
export class PaginationBar {
  readonly Icons = AppIcons;

  // Inputs van de parent
  nextPageToken = input<string | null | undefined>(null);
  isLoading = input<boolean>(false);

  loadData = output<string | undefined>();

  currentPage = signal(1);
  #tokenHistory: string[] = [];

  nextPage() {
    const token = this.nextPageToken();
    if (token) {
      this.#tokenHistory.push(token);
      this.currentPage.update((p) => p + 1);
      this.loadData.emit(token);
    }
  }

  prevPage() {
    if (this.currentPage() > 1) {
      this.#tokenHistory.pop();
      const prevToken = this.#tokenHistory.at(-1);
      this.currentPage.update((p) => p - 1);
      this.loadData.emit(prevToken);
    }
  }

  reset() {
    this.currentPage.set(1);
    this.#tokenHistory = [];
  }
}
