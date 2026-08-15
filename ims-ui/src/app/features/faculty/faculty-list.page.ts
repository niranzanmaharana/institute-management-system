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
import { StatusBadge } from '../../shared/status-badge';
import { AuthService } from '../../core/auth.service';
import { httpLoadError } from '../../core/http-error';
import { Faculty, FacultyService } from './faculty.service';

@Component({
  selector: 'app-faculty-list-page',
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
  templateUrl: './faculty-list.page.html',
  styleUrl: './faculty-list.page.scss',
})
export class FacultyListPage implements OnInit {
  private readonly facultyApi = inject(FacultyService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Faculty[]>([]);
  readonly page = signal(1);
  readonly totalPages = signal(1);
  readonly totalElements = signal(0);
  readonly pageSize = 10;

  q = '';
  status = '';
  readonly statusOptions: UiSelectOption[] = [
    { value: '', label: 'All active' },
    { value: 'ACTIVE', label: 'Active' },
    { value: 'INACTIVE', label: 'Inactive' },
  ];

  ngOnInit(): void {
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
    this.facultyApi.list(this.q, this.page() - 1, this.pageSize, this.status).subscribe({
      next: (res) => {
        this.rows.set(res.content);
        this.totalPages.set(Math.max(res.totalPages, 1));
        this.totalElements.set(res.totalElements);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'faculty'));
        this.loading.set(false);
      },
    });
  }
}
