import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  instituteId: number | null;
  roles: string[];
  permissions: string[];
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

const ACCESS_KEY = 'ims.accessToken';
const REFRESH_KEY = 'ims.refreshToken';
const USER_KEY = 'ims.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly user = signal<AuthUser | null>(this.readUser());

  login(username: string, password: string, instituteCode?: string): Observable<AuthTokens> {
    const body: Record<string, string> = { username, password };
    if (instituteCode?.trim()) {
      body['instituteCode'] = instituteCode.trim();
    }
    return this.http.post<AuthTokens>(`${environment.apiBaseUrl}/api/v1/auth/login`, body).pipe(
      tap((tokens) => this.persist(tokens)),
    );
  }

  logout(): void {
    const refreshToken = localStorage.getItem(REFRESH_KEY);
    if (refreshToken) {
      this.http
        .post(`${environment.apiBaseUrl}/api/v1/auth/logout`, { refreshToken })
        .subscribe({ error: () => undefined });
    }
    this.clearLocalSession();
    void this.router.navigateByUrl('/login');
  }

  /** Drop tokens locally without calling logout API (e.g. after 401). */
  clearLocalSession(): void {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
    this.user.set(null);
  }

  accessToken(): string | null {
    return localStorage.getItem(ACCESS_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.accessToken();
  }

  hasRole(role: string): boolean {
    return this.user()?.roles?.includes(role) ?? false;
  }

  hasPermission(permission: string): boolean {
    return this.user()?.permissions?.includes(permission) ?? false;
  }

  isPlatformAdmin(): boolean {
    return this.hasRole('PLATFORM_ADMIN');
  }

  /** Default landing route after login / guest redirect. */
  homePath(): string {
    return this.isPlatformAdmin() ? '/institutes' : '/dashboard';
  }

  private persist(tokens: AuthTokens): void {
    localStorage.setItem(ACCESS_KEY, tokens.accessToken);
    localStorage.setItem(REFRESH_KEY, tokens.refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(tokens.user));
    this.user.set(tokens.user);
  }

  private readUser(): AuthUser | null {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
