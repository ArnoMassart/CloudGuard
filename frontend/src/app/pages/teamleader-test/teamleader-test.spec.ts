import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TeamleaderTest } from './teamleader-test';

describe('TeamleaderTest', () => {
  let component: TeamleaderTest;
  let fixture: ComponentFixture<TeamleaderTest>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TeamleaderTest],
    }).compileComponents();

    fixture = TestBed.createComponent(TeamleaderTest);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
