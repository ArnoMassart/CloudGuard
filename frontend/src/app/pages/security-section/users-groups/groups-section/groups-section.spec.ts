import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupsSection } from './groups-section';

describe('GroupsSection', () => {
  let component: GroupsSection;
  let fixture: ComponentFixture<GroupsSection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupsSection]
    })
    .compileComponents();

    fixture = TestBed.createComponent(GroupsSection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
