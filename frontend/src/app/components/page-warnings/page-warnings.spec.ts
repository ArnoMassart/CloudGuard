import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageWarnings } from './page-warnings';

describe('PageWarnings', () => {
  let component: PageWarnings;
  let fixture: ComponentFixture<PageWarnings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageWarnings],
    }).compileComponents();

    fixture = TestBed.createComponent(PageWarnings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
