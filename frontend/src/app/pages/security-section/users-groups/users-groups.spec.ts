import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UsersGroups } from './users-groups';

describe('UsersGroups', () => {
  let component: UsersGroups;
  let fixture: ComponentFixture<UsersGroups>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UsersGroups]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UsersGroups);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
