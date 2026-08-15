import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { UiInput } from '../../shared/ui-input';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { RoleOption, UsersService } from './users.service';

@Component({
  selector: 'app-user-edit-page',
  imports: [ReactiveFormsModule, RouterLink, UiCard, UiButton, UiInput, ErrorState, LoadingState],
  templateUrl: './user-edit.page.html',
  styleUrl: './user-edit.page.scss',
})
export class UserEditPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly usersApi = inject(UsersService);

  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly roles = signal<RoleOption[]>([]);
  readonly username = signal('');

  private userId = 0;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    roles: this.fb.nonNullable.control<string[]>([], Validators.required),
  });

  backLink(): string {
    return this.userId > 0 ? `/users/${this.userId}` : '/users';
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid user id.');
      this.loading.set(false);
      return;
    }
    this.userId = id;
    forkJoin({
      roles: this.usersApi.listRoles(),
      user: this.usersApi.get(id),
    }).subscribe({
      next: ({ roles, user }) => {
        this.roles.set(roles);
        this.username.set(user.username);
        this.form.patchValue({
          email: user.email,
          roles: [...user.roles],
          password: '',
        });
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'user'));
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
    if (v.password && v.password.length < 8) {
      this.error.set('Password must be at least 8 characters.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.usersApi
      .update(this.userId, {
        email: v.email.trim(),
        roles: v.roles,
        password: v.password.trim() || undefined,
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          void this.router.navigate(['/users', this.userId]);
        },
        error: (err) => {
          this.submitting.set(false);
          this.error.set(httpErrorMessage(err, 'Could not update user.'));
        },
      });
  }
}
