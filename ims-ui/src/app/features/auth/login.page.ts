import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, isNetworkFailure } from '../../core/http-error';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';

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

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['admin', Validators.required],
    password: ['Password@123', Validators.required],
    instituteCode: ['DEMO_A'],
  });

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reason') === 'session') {
      this.error.set('Your session expired or is not authorized. Please sign in again.');
    }
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { username, password, instituteCode } = this.form.getRawValue();
    this.auth.login(username, password, instituteCode || undefined).subscribe({
      next: () => {
        this.submitting.set(false);
        void this.router.navigateByUrl(this.auth.homePath());
      },
      error: (err) => {
        this.submitting.set(false);
        if (isNetworkFailure(err)) {
          this.error.set(httpErrorMessage(err, 'Sign-in failed.'));
        } else if (err?.status === 401) {
          this.error.set('Invalid credentials. Use institute code DEMO_A or DEMO_B for admin.');
        } else {
          this.error.set(httpErrorMessage(err, 'Sign-in failed. Try again.'));
        }
      },
    });
  }
}
