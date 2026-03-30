import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageContentWrapper } from './page-content-wrapper';

describe('PageContentWrapper', () => {
  let component: PageContentWrapper;
  let fixture: ComponentFixture<PageContentWrapper>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageContentWrapper],
    }).compileComponents();

    fixture = TestBed.createComponent(PageContentWrapper);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
