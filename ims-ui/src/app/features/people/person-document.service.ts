import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export type PersonOwnerType = 'STUDENT' | 'FACULTY' | 'STAFF';

export interface PersonDocument {
  id: number;
  ownerType: PersonOwnerType;
  ownerId: number;
  docType: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  checksum?: string | null;
  status: string;
  uploadedBy?: number | null;
  uploadedAt: string;
}

export interface PresignedUpload {
  document: PersonDocument;
  uploadUrl: string;
  expiresAt: string;
  headers: Record<string, string>;
}

export interface PresignedDownload {
  document: PersonDocument;
  downloadUrl: string;
  expiresAt: string;
}

export interface InitDocumentUploadRequest {
  docType: string;
  fileName: string;
  contentType: string;
  fileSize: number;
}

const MAX_BYTES = 5 * 1024 * 1024;

@Injectable({ providedIn: 'root' })
export class PersonDocumentService {
  private readonly http = inject(HttpClient);

  /** Bumped after upload/delete so profile photo and document list stay in sync. */
  readonly catalogRevision = signal(0);

  notifyChanged(): void {
    this.catalogRevision.update((n) => n + 1);
  }

  private base(ownerType: PersonOwnerType, ownerId: number): string {
    return `${environment.apiBaseUrl}/api/v1/people/${ownerType}/${ownerId}/documents`;
  }

  list(ownerType: PersonOwnerType, ownerId: number): Observable<PersonDocument[]> {
    return this.http.get<PersonDocument[]>(this.base(ownerType, ownerId));
  }

  photo(ownerType: PersonOwnerType, ownerId: number): Observable<PresignedDownload> {
    return this.http.get<PresignedDownload>(`${this.base(ownerType, ownerId)}/photo`);
  }

  initUpload(
    ownerType: PersonOwnerType,
    ownerId: number,
    body: InitDocumentUploadRequest,
  ): Observable<PresignedUpload> {
    return this.http.post<PresignedUpload>(`${this.base(ownerType, ownerId)}/uploads`, body);
  }

  complete(
    ownerType: PersonOwnerType,
    ownerId: number,
    documentId: number,
    checksum?: string,
  ): Observable<PersonDocument> {
    return this.http.post<PersonDocument>(
      `${this.base(ownerType, ownerId)}/${documentId}/complete`,
      checksum ? { checksum } : {},
    );
  }

  download(
    ownerType: PersonOwnerType,
    ownerId: number,
    documentId: number,
  ): Observable<PresignedDownload> {
    return this.http.get<PresignedDownload>(
      `${this.base(ownerType, ownerId)}/${documentId}/download`,
    );
  }

  delete(ownerType: PersonOwnerType, ownerId: number, documentId: number): Observable<PersonDocument> {
    return this.http.post<PersonDocument>(
      `${this.base(ownerType, ownerId)}/${documentId}/delete`,
      {},
    );
  }

  async uploadFile(
    ownerType: PersonOwnerType,
    ownerId: number,
    file: File,
    docType: string,
  ): Promise<PersonDocument> {
    if (file.size > MAX_BYTES) {
      throw new Error('File exceeds the maximum size of 5 MB.');
    }
    const contentType = normalizeContentType(file, docType);
    if (!contentType) {
      throw new Error(
        docType === 'PHOTO'
          ? 'Profile photo must be JPEG or PNG.'
          : 'Allowed types are PDF, JPEG, and PNG.',
      );
    }
    const checksum = await sha256Hex(file);
    const init = await firstValueFrom(
      this.initUpload(ownerType, ownerId, {
        docType,
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
    const saved = await firstValueFrom(
      this.complete(ownerType, ownerId, init.document.id, checksum),
    );
    this.notifyChanged();
    return saved;
  }
}

export function normalizeContentType(file: File, docType?: string): string | null {
  const type = (file.type || '').toLowerCase();
  let normalized: string | null = null;
  if (type === 'application/pdf') {
    normalized = 'application/pdf';
  } else if (type === 'image/jpeg' || type === 'image/jpg') {
    normalized = 'image/jpeg';
  } else if (type === 'image/png') {
    normalized = 'image/png';
  } else {
    const name = file.name.toLowerCase();
    if (name.endsWith('.pdf')) {
      normalized = 'application/pdf';
    } else if (name.endsWith('.jpg') || name.endsWith('.jpeg')) {
      normalized = 'image/jpeg';
    } else if (name.endsWith('.png')) {
      normalized = 'image/png';
    }
  }
  if (docType === 'PHOTO' && normalized !== 'image/jpeg' && normalized !== 'image/png') {
    return null;
  }
  return normalized;
}

async function sha256Hex(file: File): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', await file.arrayBuffer());
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}
