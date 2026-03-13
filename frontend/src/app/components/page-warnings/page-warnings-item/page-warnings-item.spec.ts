import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageWarningsItem } from './page-warnings-item';

describe('PageWarningsItem', () => {
  let component: PageWarningsItem;
  let fixture: ComponentFixture<PageWarningsItem>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PageWarningsItem],
    }).compileComponents();

    fixture = TestBed.createComponent(PageWarningsItem);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
