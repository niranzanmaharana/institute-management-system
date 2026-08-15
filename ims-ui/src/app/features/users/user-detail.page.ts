import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage, httpLoadError } from '../../core/http-error';
import { ManagedUser, UsersService } from './users.service';

@Component({
  selector: 'app-user-detail-page',
  imports: [RouterLink, UiCard, UiButton, LoadingState, ErrorState, StatusBadge],
  templateUrl: './user-detail.page.html',
  styleUrl: './user-detail.page.scss',
})
export class UserDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly usersApi = inject(UsersService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly acting = signal(false);
  readonly error = signal<string | null>(null);
  readonly user = signal<ManagedUser | null>(null);

  private userId = 0;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set('Invalid user id.');
      this.loading.set(false);
      return;
    }
    this.userId = id;
    this.load(id);
  }

  load(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.usersApi.get(id).subscribe({
      next: (row) => {
        this.user.set(row);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'user'));
        this.loading.set(false);
      },
    });
  }

  tone(status: string): BadgeTone {
    return status === 'ACTIVE' ? 'success' : 'neutral';
  }

  isSelf(): boolean {
    return this.auth.user()?.id === this.user()?.id;
  }

  deactivate(): void {
    const row = this.user();
    if (!row || this.acting() || this.isSelf()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.usersApi.deactivate(row.id).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.acting.set(false);
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(httpErrorMessage(err, 'Could not deactivate user.'));
      },
    });
  }

  activate(): void {
    const row = this.user();
    if (!row || this.acting()) {
      return;
    }
    this.acting.set(true);
    this.error.set(null);
    this.usersApi.activate(row.id).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.acting.set(false);
      },
      error: (err) => {
        this.acting.set(false);
        this.error.set(httpErrorMessage(err, 'Could not activate user.'));
      },
    });
  }

  edit(): void {
    void this.router.navigate(['/users', this.userId, 'edit']);
  }
}
