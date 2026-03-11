import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TeamleaderAccessDenied } from './teamleader-access-denied';

describe('TeamleaderAccessDenied', () => {
  let component: TeamleaderAccessDenied;
  let fixture: ComponentFixture<TeamleaderAccessDenied>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamleaderAccessDenied],
    }).compileComponents();

    fixture = TestBed.createComponent(TeamleaderAccessDenied);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
