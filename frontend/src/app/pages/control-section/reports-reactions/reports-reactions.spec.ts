import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReportsReactions } from './reports-reactions';

describe('ReportsReactions', () => {
  let component: ReportsReactions;
  let fixture: ComponentFixture<ReportsReactions>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportsReactions]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReportsReactions);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
