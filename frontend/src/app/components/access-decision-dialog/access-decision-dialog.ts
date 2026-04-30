import { Component, inject, Inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideAngularModule } from 'lucide-angular';
import { AppIcons } from '../../shared/AppIcons';
import { User } from '../../models/users/User';
import { UserService } from '../../services/user-service';

export interface AccessDecisionDialogData {
  user: User;
  isAccepted: boolean;
}

@Component({
  selector: 'app-access-decision-dialog',
  imports: [
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    LucideAngularModule,
    TranslocoPipe,
  ],
  templateUrl: './access-decision-dialog.html',
  styleUrl: './access-decision-dialog.css',
})
export class AccessDecisionDialog {
  readonly Icons = AppIcons;

  readonly userService = inject(UserService);

  constructor(
    public dialogRef: MatDialogRef<AccessDecisionDialog>,
    @Inject(MAT_DIALOG_DATA) public data: AccessDecisionDialogData
  ) {}
}
