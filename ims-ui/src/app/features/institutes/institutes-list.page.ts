import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { Institute, InstituteService } from './institute.service';

@Component({
  selector: 'app-institutes-list-page',
  imports: [RouterLink, UiCard, UiButton, EmptyState, LoadingState, ErrorState, StatusBadge],
  templateUrl: './institutes-list.page.html',
  styleUrl: './institutes-list.page.scss',
})
export class InstitutesListPage implements OnInit {
  private readonly institutesApi = inject(InstituteService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<Institute[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.institutesApi.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'institutes'));
        this.loading.set(false);
      },
    });
  }
}
