import { Component, inject, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { Navbar } from './navbar/navbar';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('CloudGuard');
  readonly #router = inject(Router);

  showNavbar: boolean = true;

  ngOnInit(): void {
    this.#router.events.subscribe(() => {
      this.showNavbar = !this.#router.url.includes('/login');
    });
  }
}
