import { Component } from '@angular/core';
import { PageHeader } from '../../../components/page-header/page-header';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-users-groups',
  imports: [PageHeader, LucideAngularModule],
  templateUrl: './users-groups.html',
  styleUrl: './users-groups.css',
})
export class UsersGroups {}
