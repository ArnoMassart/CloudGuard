import { ChangeDetectorRef, Component, inject } from '@angular/core';
import { ApiService } from '../../services/api-service';
import { firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-api-test',
  imports: [],
  templateUrl: './api-test.html',
  styleUrl: './api-test.css',
})
export class ApiTest {
  readonly apiService: ApiService = inject(ApiService);

  readonly cdr: ChangeDetectorRef = inject(ChangeDetectorRef);

  result!: String;

  ngOnInit() {
    this.getTest();
  }

  async getTest() {
    try {
      this.result = await firstValueFrom(this.apiService.getTest());
      this.cdr.detectChanges();
    } catch (error) {
      console.error('Oops:', error);
    }
  }
}
