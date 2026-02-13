import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppAccess } from './app-access';

describe('AppAccess', () => {
  let component: AppAccess;
  let fixture: ComponentFixture<AppAccess>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppAccess]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AppAccess);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
