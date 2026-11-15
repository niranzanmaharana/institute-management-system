import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';
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

export interface InstituteBranding {
  id: number;
  code: string;
  name: string;
  iconUrl?: string | null;
  iconDownloadUrl?: string | null;
}

export interface InstituteLogoUploadInit {
  storageKey: string;
  uploadUrl: string;
  expiresAt: string;
  headers: Record<string, string>;
}

const MAX_LOGO_BYTES = 5 * 1024 * 1024;

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

  currentBranding(): Observable<InstituteBranding> {
    return this.http.get<InstituteBranding>(`${this.base}/current/branding`);
  }

  publicBranding(code: string): Observable<InstituteBranding> {
    return this.http.get<InstituteBranding>(
      `${environment.apiBaseUrl}/api/v1/public/institutes/${encodeURIComponent(code.trim())}/branding`,
    );
  }

  initLogoUpload(
    instituteId: number,
    body: { fileName: string; contentType: string; fileSize: number },
  ): Observable<InstituteLogoUploadInit> {
    return this.http.post<InstituteLogoUploadInit>(`${this.base}/${instituteId}/logo/uploads`, body);
  }

  completeLogoUpload(instituteId: number, storageKey: string): Observable<Institute> {
    return this.http.post<Institute>(`${this.base}/${instituteId}/logo/complete`, { storageKey });
  }

  async uploadLogo(instituteId: number, file: File): Promise<Institute> {
    if (file.size > MAX_LOGO_BYTES) {
      throw new Error('Logo exceeds the maximum size of 5 MB.');
    }
    const contentType = normalizeLogoType(file);
    if (!contentType) {
      throw new Error('Logo must be JPEG or PNG.');
    }
    const init = await firstValueFrom(
      this.initLogoUpload(instituteId, {
        fileName: file.name,
        contentType,
        fileSize: file.size,
      }),
    );
    const putHeaders: Record<string, string> = { ...(init.headers ?? {}) };
    if (!putHeaders['Content-Type']) {
      putHeaders['Content-Type'] = contentType;
    }
    const put = await fetch(init.uploadUrl, {
      method: 'PUT',
      headers: putHeaders,
      body: file,
    });
    if (!put.ok) {
      throw new Error(`Object storage rejected the upload (${put.status}).`);
    }
    return firstValueFrom(this.completeLogoUpload(instituteId, init.storageKey));
  }
}

export function normalizeLogoType(file: File): string | null {
  const type = (file.type || '').toLowerCase();
  if (type === 'image/jpeg' || type === 'image/jpg') {
    return 'image/jpeg';
  }
  if (type === 'image/png') {
    return 'image/png';
  }
  const name = file.name.toLowerCase();
  if (name.endsWith('.jpg') || name.endsWith('.jpeg')) {
    return 'image/jpeg';
  }
  if (name.endsWith('.png')) {
    return 'image/png';
  }
  return null;
}
