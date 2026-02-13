import { Component, inject } from '@angular/core';
import { ApiService } from '../../services/api-service';

@Component({
  standalone: true,
  selector: 'app-api-test',
  imports: [],
  templateUrl: './api-test.html',
  styleUrl: './api-test.css',
})
export class ApiTest {
  apiService: ApiService = inject(ApiService);

  result: String = '';

  ngOnInit() {
    this.apiService.getTest().subscribe((res) => {
      this.result = res.toString();
    });
  }
}
