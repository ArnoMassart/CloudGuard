import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { ApiService } from '../../services/api-service';
import { firstValueFrom } from 'rxjs';

@Component({
  standalone: true,
  selector: 'app-api-test',
  imports: [],
  templateUrl: './api-test.html',
  styleUrl: './api-test.css',
})
export class ApiTest implements OnInit {
  readonly apiService: ApiService = inject(ApiService);

  readonly cdr: ChangeDetectorRef = inject(ChangeDetectorRef);

  result!: string;

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
