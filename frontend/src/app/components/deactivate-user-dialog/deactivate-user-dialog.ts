import { Component, Inject } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';

@Component({
  selector: 'app-deactivate-user-dialog',
  imports: [
    TranslocoPipe,
    LucideAngularModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    LucideAngularModule,
    TranslocoPipe,
    CommonModule,
    FormsModule,
  ],
  templateUrl: './deactivate-user-dialog.html',
  styleUrl: './deactivate-user-dialog.css',
})
export class DeactivateUserDialog {
  readonly Icons = AppIcons;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: any,
    private dialogRef: MatDialogRef<DeactivateUserDialog>
  ) {}
}
