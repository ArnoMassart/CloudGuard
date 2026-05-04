import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResetDenialDialog } from './reset-denial-dialog';

describe('ResetDenialDialog', () => {
  let component: ResetDenialDialog;
  let fixture: ComponentFixture<ResetDenialDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ResetDenialDialog],
    }).compileComponents();

    fixture = TestBed.createComponent(ResetDenialDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
