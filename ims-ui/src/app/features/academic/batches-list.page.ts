import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { AcademicService, Batch } from './academic.service';

@Component({
  selector: 'app-batches-list-page',
  imports: [
    RouterLink,
    UiCard,
    UiButton,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './batches-list.page.html',
  styleUrl: './batches-list.page.scss',
})
export class BatchesListPage implements OnInit {
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Batch[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.academicApi.listBatches().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'batches'));
        this.loading.set(false);
      },
    });
  }
}
