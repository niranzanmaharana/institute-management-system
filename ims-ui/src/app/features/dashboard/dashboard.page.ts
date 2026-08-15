import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { UiCard } from '../../shared/ui-card';
import { StatusBadge } from '../../shared/status-badge';
import { EmptyState } from '../../shared/empty-state';
import { LoadingState } from '../../shared/loading-state';
import { ErrorState } from '../../shared/error-state';
import { httpLoadError } from '../../core/http-error';
import { AuthService } from '../../core/auth.service';

interface InstituteDto {
  id: number;
  code: string;
  name: string;
  status: string;
  timezone: string;
}

@Component({
  selector: 'app-dashboard-page',
  imports: [UiCard, StatusBadge, EmptyState, LoadingState, ErrorState],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private readonly http = inject(HttpClient);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly institutes = signal<InstituteDto[]>([]);

  ngOnInit(): void {
    this.http.get<InstituteDto[]>(`${environment.apiBaseUrl}/api/v1/institutes`).subscribe({
      next: (rows) => {
        this.institutes.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpLoadError(err, 'institutes'));
        this.loading.set(false);
      },
    });
  }
}
