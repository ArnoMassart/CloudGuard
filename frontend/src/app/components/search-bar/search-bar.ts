import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { AppIcons } from '../../shared/AppIcons';

@Component({
  selector: 'app-search-bar',
  imports: [LucideAngularModule],
  templateUrl: './search-bar.html',
  styleUrl: './search-bar.css',
})
export class SearchBar implements OnInit, OnDestroy {
  readonly Icons = AppIcons;

  @Input() placeholder: string = 'Zoek...';
  @Input() value: string = '';

  @Output() searchChange = new EventEmitter<string>();

  private readonly searchSubject = new Subject<string>();
  private subscription?: Subscription;

  ngOnInit(): void {
    this.subscription = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((value) => this.searchChange.emit(value));
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  onInput(value: string): void {
    this.searchSubject.next(value);
  }
}
