import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ErrorState } from './error-state';

describe('ErrorState', () => {
  let fixture: ComponentFixture<ErrorState>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorState],
    }).compileComponents();
    fixture = TestBed.createComponent(ErrorState);
  });

  it('exposes role=alert for assistive tech', () => {
    fixture.componentInstance.title = 'Failed';
    fixture.componentInstance.message = 'Network error';
    fixture.detectChanges();
    const alert = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
    expect(alert?.textContent).toContain('Network error');
  });
});
