import { Routes } from '@angular/router';
import { ApiTest } from './api-test/api-test';
import { Home } from './home/home';
import { Users } from './users/users';

export const routes: Routes = [
  {
    path: '',
    component: Home,
  },
  {
    path: 'users',
    component: Users,
  },
  {
    path: 'test',
    component: ApiTest,
  },
  { path: '**', redirectTo: '/' },
];
