import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { LucideAngularModule } from 'lucide-angular';
import { TranslocoPipe } from '@jsverse/transloco';
import { AppIcons } from '../../shared/AppIcons';

export interface MessageDialogData {
  titleKey: string;
  message: string;
}

@Component({
  selector: 'app-message-dialog',
  standalone: true,
  imports: [MatButtonModule, MatDialogModule, LucideAngularModule, TranslocoPipe],
  templateUrl: './message-dialog.html',
  styleUrl: './message-dialog.css',
})
export class MessageDialog {
  readonly data = inject<MessageDialogData>(MAT_DIALOG_DATA);
  readonly Icons = AppIcons;
}
