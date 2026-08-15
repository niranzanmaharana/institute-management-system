import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { UiCard } from '../../shared/ui-card';
import { UiButton } from '../../shared/ui-button';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { StatusBadge, BadgeTone } from '../../shared/status-badge';
import { httpLoadError } from '../../core/http-error';
import { ManagedUser, UsersService } from './users.service';

@Component({
  selector: 'app-users-list-page',
  imports: [RouterLink, UiCard, UiButton, EmptyState, LoadingState, ErrorState, StatusBadge],
  templateUrl: './users-list.page.html',
  styleUrl: './users-list.page.scss',
})
export class UsersListPage implements OnInit {
  private readonly usersApi = inject(UsersService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly rows = signal<ManagedUser[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.usersApi.list().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'users'));
        this.loading.set(false);
      },
    });
  }

  tone(status: string): BadgeTone {
    return status === 'ACTIVE' ? 'success' : 'neutral';
  }
}
