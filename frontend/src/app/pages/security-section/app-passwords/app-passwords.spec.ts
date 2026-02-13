import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppPasswords } from './app-passwords';

describe('AppPasswords', () => {
  let component: AppPasswords;
  let fixture: ComponentFixture<AppPasswords>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppPasswords]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppPasswords);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
