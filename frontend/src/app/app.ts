import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ApiTest } from './api-test/api-test';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ApiTest],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('frontend');
}
