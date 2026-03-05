import { Component, inject, signal } from '@angular/core';
import { TeamleaderService } from '../../services/teamleader-service';
import { JsonPipe } from '@angular/common';

@Component({
  selector: 'app-teamleader-test',
  imports: [JsonPipe],
  templateUrl: './teamleader-test.html',
  styleUrl: './teamleader-test.css',
})
export class TeamleaderTest {
  readonly rawData = signal<any>(null);
  readonly isLoading = signal(false);

  readonly #tlService = inject(TeamleaderService);

  fetchData() {
    this.isLoading.set(true);
    this.#tlService.getTestCompanyData().subscribe({
      next: (data) => {
        this.rawData.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('API Error:', err);
        this.rawData.set({ error: 'Verzoek mislukt', details: err.message });
        this.isLoading.set(false);
      },
    });
  }
}
