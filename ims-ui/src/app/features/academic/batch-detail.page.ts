import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { AcademicService, Batch } from './academic.service';

@Component({
  selector: 'app-batch-detail-page',
  imports: [RouterLink, UiCard, UiButton, LoadingState, ErrorState, StatusBadge],
  templateUrl: './batch-detail.page.html',
  styleUrl: './batch-detail.page.scss',
})
export class BatchDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicApi = inject(AcademicService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly batch = signal<Batch | null>(null);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid batch id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.academicApi.getBatch(id).subscribe({
      next: (row) => {
        this.batch.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'batch'));
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    const row = this.batch();
    if (row) {
      void this.router.navigate(['/batches', row.id, 'edit']);
    }
  }
}
