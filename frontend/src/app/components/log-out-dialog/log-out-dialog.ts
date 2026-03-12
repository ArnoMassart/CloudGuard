import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';
import { AppIcons } from '../../shared/AppIcons';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-log-out-dialog',
  imports: [MatButtonModule, MatCheckboxModule, MatDialogModule, LucideAngularModule],
  templateUrl: './log-out-dialog.html',
  styleUrl: './log-out-dialog.css',
})
export class LogOutDialog {
  readonly Icons = AppIcons;
}
