import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { RoleOption, UsersService } from './users.service';

@Component({
  selector: 'app-user-create-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './user-create.page.html',
  styleUrl: './user-create.page.scss',
})
export class UserCreatePage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly usersApi = inject(UsersService);
  private readonly router = inject(Router);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly roles = signal<RoleOption[]>([]);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(64)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    roles: this.fb.nonNullable.control<string[]>([], Validators.required),
  });

  ngOnInit(): void {
    this.usersApi.listRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'roles'));
        this.loading.set(false);
      },
    });
  }

  toggleRole(code: string, checked: boolean): void {
    const current = [...this.form.controls.roles.value];
    if (checked && !current.includes(code)) {
      current.push(code);
    } else if (!checked) {
      const idx = current.indexOf(code);
      if (idx >= 0) {
        current.splice(idx, 1);
      }
    }
    this.form.controls.roles.setValue(current);
    this.form.controls.roles.markAsTouched();
  }

  isChecked(code: string): boolean {
    return this.form.controls.roles.value.includes(code);
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    if (v.roles.length === 0) {
      this.error.set('Select at least one role.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.usersApi
      .create({
        username: v.username.trim(),
        email: v.email.trim(),
        password: v.password,
        roles: v.roles,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigateByUrl('/users');
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not create user.'));
        },
      });
  }
}
