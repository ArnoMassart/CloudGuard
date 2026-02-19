import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsersSectionTopCard } from './users-section-top-card';

describe('UsersSectionTopCard', () => {
  let component: UsersSectionTopCard;
  let fixture: ComponentFixture<UsersSectionTopCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersSectionTopCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UsersSectionTopCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
