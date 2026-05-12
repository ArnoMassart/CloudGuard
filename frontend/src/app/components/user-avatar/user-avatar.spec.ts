import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserAvatar } from './user-avatar';

describe('UserAvatar', () => {
  let fixture: ComponentFixture<UserAvatar>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserAvatar],
    }).compileComponents();

    fixture = TestBed.createComponent(UserAvatar);
  });

  function setInputs(pictureUrl: string | null | undefined, initials: string) {
    fixture.componentRef.setInput('pictureUrl', pictureUrl);
    fixture.componentRef.setInput('initials', initials);
    fixture.detectChanges();
  }

  it('should create', () => {
    setInputs(undefined, 'AB');
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders img when pictureUrl is set', () => {
    setInputs('https://example.com/photo.jpg', 'AB');
    const img = fixture.nativeElement.querySelector('img');
    expect(img).toBeTruthy();
    expect(img.getAttribute('src')).toContain('example.com');
  });

  it('renders initials when pictureUrl is absent', () => {
    setInputs(undefined, 'CD');
    expect(fixture.nativeElement.textContent).toContain('CD');
    expect(fixture.nativeElement.querySelector('img')).toBeFalsy();
  });

  it('falls back to initials after image load error', () => {
    setInputs('https://broken.example/bad.png', 'ZZ');
    const img = fixture.nativeElement.querySelector('img') as HTMLImageElement;
    expect(img).toBeTruthy();
    img.dispatchEvent(new Event('error'));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('ZZ');
    expect(fixture.nativeElement.querySelector('img')).toBeFalsy();
  });

  it('shows img again after pictureUrl changes following an error', () => {
    setInputs('https://broken.example/bad.png', 'ZZ');
    fixture.nativeElement.querySelector('img')!.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeFalsy();

    fixture.componentRef.setInput('pictureUrl', 'https://ok.example/good.png');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeTruthy();
  });
});
