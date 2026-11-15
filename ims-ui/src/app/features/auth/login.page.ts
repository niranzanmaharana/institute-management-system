import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, isNetworkFailure } from '../../core/http-error';
import { ToastService } from '../../shared/toast.service';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { InstituteService } from '../institutes/institute.service';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, UiButton, UiInput],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toasts = inject(ToastService);
  private readonly institutesApi = inject(InstituteService);
  private readonly destroyRef = inject(DestroyRef);

  readonly submitting = signal(false);
  readonly brandName = signal('Institute Management System');
  readonly brandLogoUrl = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['admin', Validators.required],
    password: ['Password@123', Validators.required],
    instituteCode: ['DEMO_A'],
  });

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reason') === 'session') {
      this.toasts.warning('Your session expired or is not authorized. Please sign in again.');
    }
    this.form.controls.instituteCode.valueChanges
      .pipe(
        debounceTime(350),
        distinctUntilChanged(),
        switchMap((code) => {
          const trimmed = (code || '').trim();
          if (!trimmed) {
            this.brandName.set('Institute Management System');
            this.brandLogoUrl.set(null);
            return of(null);
          }
          return this.institutesApi.publicBranding(trimmed).pipe(
            catchError(() => {
              this.brandName.set('Institute Management System');
              this.brandLogoUrl.set(null);
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((branding) => {
        if (!branding) {
          return;
        }
        this.brandName.set(branding.name);
        this.brandLogoUrl.set(branding.iconDownloadUrl || null);
      });
    const initial = this.form.controls.instituteCode.value?.trim();
    if (initial) {
      this.institutesApi.publicBranding(initial).subscribe({
        next: (branding) => {
          this.brandName.set(branding.name);
          this.brandLogoUrl.set(branding.iconDownloadUrl || null);
        },
        error: () => undefined,
      });
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    const { username, password, instituteCode } = this.form.getRawValue();
    this.auth.login(username, password, instituteCode || undefined).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.auth.homePath());
      },
      error: (err) => {
        this.submitting.set(false);
        if (isNetworkFailure(err)) {
          this.toasts.danger(httpErrorMessage(err, 'Sign-in failed.'));
        } else if (err?.status === 401) {
          this.toasts.danger('Invalid credentials. Use institute code DEMO_A or DEMO_B for admin.');
        } else {
          this.toasts.danger(httpErrorMessage(err, 'Sign-in failed. Try again.'));
        }
      },
    });
  }
}
