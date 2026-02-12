import { Routes } from '@angular/router';
import { ApiTest } from './api-test/api-test';
import { Home } from './home/home';

export const routes: Routes = [
  {
    path: '',
    component: Home,
  },
  {
    path: 'test',
    component: ApiTest,
  },
  { path: '**', redirectTo: '/' },
];
