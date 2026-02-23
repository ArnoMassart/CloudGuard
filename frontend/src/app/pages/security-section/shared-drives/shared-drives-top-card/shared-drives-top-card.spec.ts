import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SharedDrivesTopCard } from './shared-drives-top-card';

describe('SharedDrivesTopCard', () => {
  let component: SharedDrivesTopCard;
  let fixture: ComponentFixture<SharedDrivesTopCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SharedDrivesTopCard]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SharedDrivesTopCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
