import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { PersonDocumentsSection } from '../people/person-documents.section';
import { PersonPhotoComponent } from '../people/person-photo.component';
import { Faculty, FacultyService } from './faculty.service';

@Component({
  selector: 'app-faculty-detail-page',
  imports: [
    RouterLink,
    UiCard,
    UiButton,
    LoadingState,
    ErrorState,
    StatusBadge,
    PersonDocumentsSection,
    PersonPhotoComponent,
  ],
  templateUrl: './faculty-detail.page.html',
  styleUrl: './faculty-detail.page.scss',
})
export class FacultyDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly facultyApi = inject(FacultyService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly faculty = signal<Faculty | null>(null);
  readonly deactivating = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid faculty id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.facultyApi.get(id).subscribe({
      next: (row) => {
        this.faculty.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'faculty'));
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    const row = this.faculty();
    if (row) {
      void this.router.navigate(['/faculty', row.id, 'edit']);
    }
  }

  deactivate(): void {
    const row = this.faculty();
    if (!row || row.status !== 'ACTIVE' || this.deactivating()) {
      return;
    }
    if (!confirm(`Deactivate faculty ${row.facultyCode}?`)) {
      return;
    }
    this.deactivating.set(true);
    this.facultyApi.deactivate(row.id).subscribe({
      next: (updated) => {
        this.faculty.set(updated);
        this.deactivating.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not deactivate faculty.'));
        this.deactivating.set(false);
      },
    });
  }
}
