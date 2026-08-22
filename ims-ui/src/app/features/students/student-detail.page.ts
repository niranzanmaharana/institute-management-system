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
import { Student, StudentService } from './student.service';

@Component({
  selector: 'app-student-detail-page',
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
  templateUrl: './student-detail.page.html',
  styleUrl: './student-detail.page.scss',
})
export class StudentDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly studentsApi = inject(StudentService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly student = signal<Student | null>(null);
  readonly deactivating = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid student id.');
      this.loading.set(false);
      return;
    }
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.studentsApi.get(id).subscribe({
      next: (row) => {
        this.student.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'student'));
        this.loading.set(false);
      },
    });
  }

  edit(): void {
    const row = this.student();
    if (row) {
      void this.router.navigate(['/students', row.id, 'edit']);
    }
  }

  deactivate(): void {
    const row = this.student();
    if (!row || row.status !== 'ACTIVE' || this.deactivating()) {
      return;
    }
    if (!confirm(`Deactivate student ${row.studentCode}?`)) {
      return;
    }
    this.deactivating.set(true);
    this.studentsApi.deactivate(row.id).subscribe({
      next: (updated) => {
        this.student.set(updated);
        this.deactivating.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not deactivate student.'));
        this.deactivating.set(false);
      },
    });
  }
}
