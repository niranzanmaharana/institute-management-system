import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { Institute, InstituteService } from './institute.service';

@Component({
  selector: 'app-institute-detail-page',
  imports: [RouterLink, UiCard, UiButton, LoadingState, ErrorState, StatusBadge],
  templateUrl: './institute-detail.page.html',
  styleUrl: './institute-detail.page.scss',
})
export class InstituteDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly institutesApi = inject(InstituteService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly institute = signal<Institute | null>(null);
  readonly suspending = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid institute id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.institutesApi.get(id).subscribe({
      next: (row) => {
        this.institute.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'institute'));
        this.loading.set(false);
      },
    });
  }

  suspend(): void {
    const row = this.institute();
    if (!row || row.status === 'SUSPENDED' || this.suspending()) {
      return;
    }
    if (!confirm(`Suspend institute ${row.code}? Users in this tenant will be blocked from login.`)) {
      return;
    }
    this.suspending.set(true);
    this.institutesApi.suspend(row.id).subscribe({
      next: (updated) => {
        this.institute.set(updated);
        this.suspending.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, `Could not suspend ${row.code}.`));
        this.suspending.set(false);
      },
    });
  }

  edit(): void {
    const row = this.institute();
    if (row) {
      void this.router.navigate(['/institutes', row.id, 'edit']);
    }
  }
}
