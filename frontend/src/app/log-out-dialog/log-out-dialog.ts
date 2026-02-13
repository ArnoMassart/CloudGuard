import { Component, NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule } from '@angular/material/dialog';

@Component({
  selector: 'app-log-out-dialog',
  imports: [MatButtonModule, MatCheckboxModule, MatDialogModule],
  templateUrl: './log-out-dialog.html',
  styleUrl: './log-out-dialog.css',
})
export class LogOutDialog {}
