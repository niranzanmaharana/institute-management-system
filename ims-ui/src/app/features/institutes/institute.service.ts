import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Institute {
  id: number;
  code: string;
  name: string;
  status: string;
  timezone: string;
  mobile?: string | null;
  adminEmail?: string | null;
  website?: string | null;
  addressLine1?: string | null;
  addressLine2?: string | null;
  city?: string | null;
  state?: string | null;
  postalCode?: string | null;
  country?: string | null;
  iconUrl?: string | null;
}

export interface InstituteProfilePayload {
  mobile?: string;
  adminEmail?: string;
  website?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  country?: string;
  iconUrl?: string;
}

export interface CreateInstitutePayload {
  code?: string;
  name: string;
  timezone?: string;
  profile?: InstituteProfilePayload;
  initialAdmin?: {
    username: string;
    email: string;
    password?: string;
  };
}

export interface UpdateInstitutePayload {
  name: string;
  timezone?: string;
  profile?: InstituteProfilePayload;
}

@Injectable({ providedIn: 'root' })
export class InstituteService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/institutes`;

  list(): Observable<Institute[]> {
    return this.http.get<Institute[]>(this.base);
  }

  get(id: number): Observable<Institute> {
    return this.http.get<Institute>(`${this.base}/${id}`);
  }

  create(payload: CreateInstitutePayload): Observable<Institute> {
    return this.http.post<Institute>(this.base, payload);
  }

  update(id: number, payload: UpdateInstitutePayload): Observable<Institute> {
    return this.http.put<Institute>(`${this.base}/${id}`, payload);
  }

  suspend(id: number): Observable<Institute> {
    return this.http.post<Institute>(`${this.base}/${id}/suspend`, {});
  }
}
