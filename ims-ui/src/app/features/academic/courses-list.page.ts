import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { AcademicService, Course } from './academic.service';

@Component({
  selector: 'app-courses-list-page',
  imports: [
    RouterLink,
    FormsModule,
    UiCard,
    UiButton,
    UiInput,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './courses-list.page.html',
  styleUrl: './courses-list.page.scss',
})
export class CoursesListPage implements OnInit {
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Course[]>([]);
  q = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.academicApi.listCourses(this.q).subscribe({
      next: (page) => {
        this.rows.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'courses'));
        this.loading.set(false);
      },
    });
  }
}
