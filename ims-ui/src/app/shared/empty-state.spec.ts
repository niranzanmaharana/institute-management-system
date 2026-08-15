import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyState } from './empty-state';

describe('EmptyState', () => {
  let fixture: ComponentFixture<EmptyState>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyState],
    }).compileComponents();
    fixture = TestBed.createComponent(EmptyState);
  });

  it('renders title and message', () => {
    fixture.componentInstance.title = 'No students';
    fixture.componentInstance.message = 'Add a student to get started.';
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No students');
    expect(text).toContain('Add a student to get started.');
  });
});
