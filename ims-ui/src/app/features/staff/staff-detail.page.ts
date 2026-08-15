import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { Staff, StaffService } from './staff.service';

@Component({
  selector: 'app-staff-detail-page',
  imports: [RouterLink, UiCard, UiButton, LoadingState, ErrorState, StatusBadge],
  templateUrl: './staff-detail.page.html',
  styleUrl: './staff-detail.page.scss',
})
export class StaffDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly staffApi = inject(StaffService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly staff = signal<Staff | null>(null);
  readonly deactivating = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid staff id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.staffApi.get(id).subscribe({
      next: (row) => {
        this.staff.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'staff'));
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    const row = this.staff();
    if (row) {
      void this.router.navigate(['/staff', row.id, 'edit']);
    }
  }

  deactivate(): void {
    const row = this.staff();
    if (!row || row.status !== 'ACTIVE' || this.deactivating()) {
      return;
    }
    if (!confirm(`Deactivate staff ${row.staffCode}?`)) {
      return;
    }
    this.deactivating.set(true);
    this.staffApi.deactivate(row.id).subscribe({
      next: (updated) => {
        this.staff.set(updated);
        this.deactivating.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not deactivate staff.'));
        this.deactivating.set(false);
      },
    });
  }
}
