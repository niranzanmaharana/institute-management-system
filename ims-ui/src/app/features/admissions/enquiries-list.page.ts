import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { UiSelect, UiSelectOption } from '../../shared/ui-select';
import { UiPagination } from '../../shared/ui-pagination';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { AuthService } from '../../core/auth.service';
import { httpLoadError } from '../../core/http-error';
import { AcademicService, Course } from '../academic/academic.service';
import { Enquiry, EnquiryService } from './enquiry.service';

@Component({
  selector: 'app-enquiries-list-page',
  imports: [
    RouterLink,
    FormsModule,
    UiCard,
    UiButton,
    UiInput,
    UiSelect,
    UiPagination,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './enquiries-list.page.html',
  styleUrl: './enquiries-list.page.scss',
})
export class EnquiriesListPage implements OnInit {
  private readonly enquiriesApi = inject(EnquiryService);
  private readonly academicApi = inject(AcademicService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Enquiry[]>([]);
  readonly courses = signal<Course[]>([]);
  readonly page = signal(1);
  readonly totalPages = signal(1);
  readonly totalElements = signal(0);
  readonly pageSize = 10;

  q = '';
  status = 'OPEN';
  readonly statusOptions: UiSelectOption[] = [
    { value: '', label: 'All statuses' },
    { value: 'OPEN', label: 'Open' },
    { value: 'CONVERTED', label: 'Converted' },
    { value: 'CLOSED', label: 'Closed' },
  ];

  ngOnInit(): void {
    this.academicApi.listCourses('', 0, 100).subscribe({
      next: (page) => this.courses.set(page.content),
      error: () => this.courses.set([]),
    });
    this.reload();
  }

  search(): void {
    this.page.set(1);
    this.reload();
  }

  onPageChange(next: number): void {
    this.page.set(next);
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.enquiriesApi.list(this.q, this.page() - 1, this.pageSize, this.status).subscribe({
      next: (res) => {
        this.rows.set(res.content);
        this.totalPages.set(Math.max(res.totalPages, 1));
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'enquiries'));
        this.loading.set(false);
      },
    });
  }

  courseLabel(courseId?: number): string {
    if (!courseId) {
      return '—';
    }
    const course = this.courses().find((c) => c.id === courseId);
    return course ? `${course.code} — ${course.name}` : `#${courseId}`;
  }

  tone(status: string): BadgeTone {
    if (status === 'CONVERTED') return 'success';
    if (status === 'CLOSED') return 'neutral';
    return 'info';
  }
}
