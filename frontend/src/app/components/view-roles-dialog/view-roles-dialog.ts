import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AppIcons } from '../../shared/AppIcons';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';

export interface ViewRolesDialogData {
  roles: { value: any; label: string }[];
}

@Component({
  selector: 'app-view-roles-dialog',
  imports: [TranslocoPipe, LucideAngularModule],
  templateUrl: './view-roles-dialog.html',
  styleUrl: './view-roles-dialog.css',
})
export class ViewRolesDialog {
  readonly Icons = AppIcons;

  constructor(
    public dialogRef: MatDialogRef<ViewRolesDialog>,
    @Inject(MAT_DIALOG_DATA) public data: ViewRolesDialogData
  ) {}

  // 2. Maak de sluit-functie
  closeDialog(): void {
    this.dialogRef.close();
  }
}
