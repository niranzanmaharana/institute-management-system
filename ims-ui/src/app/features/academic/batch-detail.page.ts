import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { EmptyState } from '../../shared/empty-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';
import { Faculty, FacultyService } from '../faculty/faculty.service';
import {
  AcademicService,
  Batch,
  CourseSubject,
  FacultyAssignment,
} from './academic.service';

@Component({
  selector: 'app-batch-detail-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    LoadingState,
    ErrorState,
    StatusBadge,
    EmptyState,
  ],
  templateUrl: './batch-detail.page.html',
  styleUrl: './batch-detail.page.scss',
})
export class BatchDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly academicApi = inject(AcademicService);
  private readonly facultyApi = inject(FacultyService);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly batch = signal<Batch | null>(null);
  readonly assignments = signal<FacultyAssignment[]>([]);
  readonly subjects = signal<CourseSubject[]>([]);
  readonly faculties = signal<Faculty[]>([]);
  readonly assignError = signal<string | null>(null);
  readonly assigning = signal(false);
  readonly removingId = signal<number | null>(null);

  private batchId = 0;

  readonly assignForm = this.fb.nonNullable.group({
    facultyId: ['', Validators.required],
    subjectId: [''],
    role: ['TEACHER'],
  });

  get canAssign(): boolean {
    return this.auth.hasPermission('course:write');
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid batch id.');
      this.loading.set(false);
      return;
    }
    this.batchId = id;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.academicApi.getBatch(this.batchId).subscribe({
      next: (row) => {
        this.batch.set(row);
        this.loadRelated(row.courseId);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'batch'));
        this.loading.set(false);
      },
    });
  }

  loadRelated(courseId: number): void {
    forkJoin({
      assignments: this.academicApi.listFacultyAssignments(this.batchId),
      subjects: this.academicApi.listSubjects(courseId),
      faculties: this.facultyApi.list('', 0, 100, 'ACTIVE'),
    }).subscribe({
      next: (data) => {
        this.assignments.set(data.assignments);
        this.subjects.set(data.subjects);
        this.faculties.set(data.faculties.content);
        this.loading.set(false);
      },
      error: (err) => {
        if (err?.status === 403) {
          this.academicApi.listFacultyAssignments(this.batchId).subscribe({
            next: (rows) => {
              this.assignments.set(rows);
              this.loading.set(false);
            },
            error: (inner) => {
              this.error.set(httpLoadError(inner, 'assignments'));
              this.loading.set(false);
            },
          });
          return;
        }
        this.error.set(httpLoadError(err, 'assignments'));
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

  assign(): void {
    if (!this.canAssign || this.assignForm.invalid || this.assigning()) {
      this.assignForm.markAllAsTouched();
      return;
    }
    const v = this.assignForm.getRawValue();
    const facultyId = Number(v.facultyId);
    const subjectRaw = v.subjectId.trim();
    const subjectId = subjectRaw ? Number(subjectRaw) : null;
    this.assigning.set(true);
    this.assignError.set(null);
    this.academicApi
      .assignFaculty(this.batchId, {
        facultyId,
        subjectId,
        role: v.role.trim() || 'TEACHER',
      })
      .subscribe({
        next: (row) => {
          this.assignments.update((list) => [row, ...list]);
          this.assignForm.patchValue({ facultyId: '', subjectId: '', role: 'TEACHER' });
          this.assigning.set(false);
        },
        error: (err) => {
          this.assigning.set(false);
          if (err?.status === 409) {
            this.assignError.set('This faculty is already assigned for that subject.');
          } else {
            this.assignError.set(httpErrorMessage(err, 'Could not assign faculty.'));
          }
        },
      });
  }

  remove(assignment: FacultyAssignment): void {
    if (!this.canAssign || this.removingId() != null) {
      return;
    }
    this.removingId.set(assignment.id);
    this.assignError.set(null);
    this.academicApi.removeFacultyAssignment(this.batchId, assignment.id).subscribe({
      next: () => {
        this.assignments.update((list) => list.filter((row) => row.id !== assignment.id));
        this.removingId.set(null);
      },
      error: (err) => {
        this.removingId.set(null);
        this.assignError.set(httpErrorMessage(err, 'Could not remove assignment.'));
      },
    });
  }
}
