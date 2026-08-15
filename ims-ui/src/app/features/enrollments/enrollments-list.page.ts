import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { Enrollment, EnrollmentService } from './enrollment.service';

@Component({
  selector: 'app-enrollments-list-page',
  imports: [
    RouterLink,
    UiCard,
    UiButton,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './enrollments-list.page.html',
  styleUrl: './enrollments-list.page.scss',
})
export class EnrollmentsListPage implements OnInit {
  private readonly enrollmentsApi = inject(EnrollmentService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Enrollment[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.enrollmentsApi.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'enrollments'));
        this.loading.set(false);
      },
    });
  }

  tone(status: string): BadgeTone {
    if (status === 'ACTIVE') return 'success';
    if (status === 'APPLIED') return 'info';
    if (status === 'SUSPENDED') return 'warning';
    if (status === 'WITHDRAWN' || status === 'CANCELLED') return 'danger';
    return 'neutral';
  }
}
