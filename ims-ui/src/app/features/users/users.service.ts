import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface RoleOption {
  code: string;
  name: string;
}

export interface ManagedUser {
  id: number;
  username: string;
  email: string;
  status: string;
  roles: string[];
  lastLoginAt?: string;
  createdAt: string;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  roles: string[];
}

export interface UpdateUserRequest {
  email: string;
  roles: string[];
  password?: string;
}

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listRoles(): Observable<RoleOption[]> {
    return this.http.get<RoleOption[]>(`${this.base}/roles`);
  }

  list(): Observable<ManagedUser[]> {
    return this.http.get<ManagedUser[]>(`${this.base}/users`);
  }

  get(id: number): Observable<ManagedUser> {
    return this.http.get<ManagedUser>(`${this.base}/users/${id}`);
  }

  create(body: CreateUserRequest): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.base}/users`, body);
  }

  update(id: number, body: UpdateUserRequest): Observable<ManagedUser> {
    return this.http.put<ManagedUser>(`${this.base}/users/${id}`, body);
  }

  deactivate(id: number): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.base}/users/${id}/deactivate`, {});
  }

  activate(id: number): Observable<ManagedUser> {
    return this.http.post<ManagedUser>(`${this.base}/users/${id}/activate`, {});
  }
}
