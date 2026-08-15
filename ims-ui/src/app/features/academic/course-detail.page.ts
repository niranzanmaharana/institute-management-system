import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge } from '../../shared/status-badge';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { CodeService } from '../../shared/code.service';
import { AcademicService, Course, FeeCategory, FeePlan } from './academic.service';

@Component({
  selector: 'app-course-detail-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    UiCard,
    UiButton,
    UiInput,
    EmptyState,
    LoadingState,
    ErrorState,
    StatusBadge,
  ],
  templateUrl: './course-detail.page.html',
  styleUrl: './course-detail.page.scss',
})
export class CourseDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly academicApi = inject(AcademicService);
  private readonly codesApi = inject(CodeService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly course = signal<Course | null>(null);
  readonly feePlans = signal<FeePlan[]>([]);
  readonly categories = signal<FeeCategory[]>([]);

  readonly categorySubmitting = signal(false);
  readonly categoryError = signal<string | null>(null);
  readonly planSubmitting = signal(false);
  readonly planError = signal<string | null>(null);
  readonly generatingCategory = signal(false);
  readonly generatingPlan = signal(false);
  readonly deactivating = signal(false);

  private courseId = 0;

  readonly categoryForm = this.fb.nonNullable.group({
    code: [''],
    name: ['', Validators.required],
  });

  readonly planForm = this.fb.nonNullable.group({
    code: [''],
    name: ['', Validators.required],
    currency: ['INR'],
    totalAmount: ['', Validators.required],
    feeCategoryId: ['', Validators.required],
    installmentLabel: ['Full fee', Validators.required],
    dueOffsetDays: ['0'],
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid course id.');
      this.loading.set(false);
      return;
    }
    this.courseId = id;
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.academicApi.getCourse(this.courseId).subscribe({
      next: (course) => {
        this.course.set(course);
        this.loadFeePlans();
        this.loadCategories();
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'course'));
        this.loading.set(false);
      },
    });
  }

  loadFeePlans(): void {
    this.academicApi.listFeePlans(this.courseId).subscribe({
      next: (plans) => this.feePlans.set(plans),
      error: (err) => this.planError.set(httpLoadError(err, 'fee plans')),
    });
  }

  loadCategories(): void {
    this.academicApi.listFeeCategories().subscribe({
      next: (cats) => {
        this.categories.set(cats);
        if (cats.length > 0 && !this.planForm.controls.feeCategoryId.value) {
          this.planForm.controls.feeCategoryId.setValue(String(cats[0].id));
        }
      },
      error: (err) => this.categoryError.set(httpLoadError(err, 'fee categories')),
    });
  }

  deactivate(): void {
    const row = this.course();
    if (!row || row.status !== 'ACTIVE' || this.deactivating()) {
      return;
    }
    if (!confirm(`Deactivate course ${row.code}?`)) {
      return;
    }
    this.deactivating.set(true);
    this.academicApi.deactivateCourse(row.id).subscribe({
      next: (updated) => {
        this.course.set(updated);
        this.deactivating.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not deactivate course.'));
        this.deactivating.set(false);
      },
    });
  }

  createCategory(): void {
    if (this.categoryForm.invalid || this.categorySubmitting()) {
      this.categoryForm.markAllAsTouched();
      return;
    }
    this.categorySubmitting.set(true);
    this.categoryError.set(null);
    const v = this.categoryForm.getRawValue();
    this.academicApi
      .createFeeCategory({ code: v.code.trim() || undefined, name: v.name.trim() })
      .subscribe({
        next: (cat) => {
          this.categories.update((list) => [...list, cat]);
          this.planForm.controls.feeCategoryId.setValue(String(cat.id));
          this.categoryForm.reset({ code: '', name: '' });
          this.categorySubmitting.set(false);
        },
        error: (err) => {
          this.categorySubmitting.set(false);
          if (err?.status === 409) {
            this.categoryError.set('Fee category code already exists.');
          } else {
            this.categoryError.set(httpErrorMessage(err, 'Could not create fee category.'));
          }
        },
      });
  }

  generateCategoryCode(): void {
    if (this.generatingCategory()) {
      return;
    }
    this.generatingCategory.set(true);
    this.categoryError.set(null);
    this.codesApi.next('FEE_CATEGORY').subscribe({
      next: (res) => {
        this.categoryForm.controls.code.setValue(res.code);
        this.generatingCategory.set(false);
      },
      error: (err) => {
        this.generatingCategory.set(false);
        this.categoryError.set(httpErrorMessage(err, 'Could not generate category code.'));
      },
    });
  }

  generatePlanCode(): void {
    if (this.generatingPlan()) {
      return;
    }
    this.generatingPlan.set(true);
    this.planError.set(null);
    this.codesApi.next('FEE_PLAN').subscribe({
      next: (res) => {
        this.planForm.controls.code.setValue(res.code);
        this.generatingPlan.set(false);
      },
      error: (err) => {
        this.generatingPlan.set(false);
        this.planError.set(httpErrorMessage(err, 'Could not generate fee plan code.'));
      },
    });
  }

  createFeePlan(): void {
    if (this.planForm.invalid || this.planSubmitting() || this.categories().length === 0) {
      this.planForm.markAllAsTouched();
      return;
    }
    const v = this.planForm.getRawValue();
    const totalAmount = Number(v.totalAmount);
    const feeCategoryId = Number(v.feeCategoryId);
    const dueOffsetDays = Number(v.dueOffsetDays || '0');
    if (!Number.isFinite(totalAmount) || totalAmount <= 0) {
      this.planError.set('Total amount must be a positive number.');
      return;
    }
    if (!Number.isFinite(feeCategoryId) || feeCategoryId <= 0) {
      this.planError.set('Select a fee category.');
      return;
    }

    this.planSubmitting.set(true);
    this.planError.set(null);
    this.academicApi
      .createFeePlan(this.courseId, {
        code: v.code.trim() || undefined,
        name: v.name.trim(),
        currency: v.currency.trim() || 'INR',
        totalAmount,
        installments: [
          {
            seq: 1,
            feeCategoryId,
            label: v.installmentLabel.trim() || 'Full fee',
            amount: totalAmount,
            dueOffsetDays: Number.isFinite(dueOffsetDays) ? dueOffsetDays : 0,
          },
        ],
      })
      .subscribe({
        next: (plan) => {
          this.feePlans.update((list) => [...list, plan]);
          this.planForm.reset({
            code: '',
            name: '',
            currency: 'INR',
            totalAmount: '',
            feeCategoryId: String(feeCategoryId),
            installmentLabel: 'Full fee',
            dueOffsetDays: '0',
          });
          this.planSubmitting.set(false);
        },
        error: (err) => {
          this.planSubmitting.set(false);
          if (err?.status === 409) {
            this.planError.set('Fee plan code already exists for this course.');
          } else if (err?.status === 400) {
            this.planError.set('Invalid fee plan. Check amounts and installments.');
          } else {
            this.planError.set(httpErrorMessage(err, 'Could not create fee plan.'));
          }
        },
      });
  }
}
