import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsersSection } from './users-section';

describe('UsersSection', () => {
  let component: UsersSection;
  let fixture: ComponentFixture<UsersSection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersSection]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UsersSection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
