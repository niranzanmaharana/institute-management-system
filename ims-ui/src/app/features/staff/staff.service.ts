import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Staff {
  id: number;
  staffCode: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  designation?: string;
  status: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CreateStaffRequest {
  staffCode?: string;
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  designation?: string;
}

export interface UpdateStaffRequest {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  designation?: string;
}

@Injectable({ providedIn: 'root' })
export class StaffService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/staff`;

  list(q = '', page = 0, size = 20, status = ''): Observable<PageResponse<Staff>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (q.trim()) {
      params = params.set('q', q.trim());
    }
    if (status.trim()) {
      params = params.set('status', status.trim());
    }
    return this.http.get<PageResponse<Staff>>(this.base, { params });
  }

  get(id: number): Observable<Staff> {
    return this.http.get<Staff>(`${this.base}/${id}`);
  }

  create(body: CreateStaffRequest): Observable<Staff> {
    return this.http.post<Staff>(this.base, body);
  }

  update(id: number, body: UpdateStaffRequest): Observable<Staff> {
    return this.http.put<Staff>(`${this.base}/${id}`, body);
  }

  deactivate(id: number): Observable<Staff> {
    return this.http.post<Staff>(`${this.base}/${id}/deactivate`, {});
  }
}
