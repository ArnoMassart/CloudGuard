import { CommonModule } from '@angular/common';
import { Component, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-reset-denial-dialog',
  imports: [MatButtonModule, MatDialogModule, LucideAngularModule, TranslocoPipe, CommonModule],
  templateUrl: './reset-denial-dialog.html',
  styleUrl: './reset-denial-dialog.css',
})
export class ResetDenialDialog {
  readonly Icons = AppIcons;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dialogRef: MatDialogRef<ResetDenialDialog>
  ) {}

  confirm() {
    this.dialogRef.close({
      confirmed: true,
      userEmail: this.data.user.email,
    });
  }
}
