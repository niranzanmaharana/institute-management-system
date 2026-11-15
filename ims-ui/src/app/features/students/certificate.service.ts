import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CertificateTemplate {
  code: string;
  name: string;
  active: boolean;
}

export interface GeneratedCertificate {
  id: number;
  templateCode: string;
  studentId: number;
  enrollmentId?: number | null;
  fileName: string;
  contentType: string;
  fileSize: number;
  status: string;
  issuedAt: string;
  issuedByUserId?: number | null;
}

export interface CertificateDownload {
  url: string;
  expiresInSeconds: number;
}

@Injectable({ providedIn: 'root' })
export class CertificateService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1`;

  listTemplates(): Observable<CertificateTemplate[]> {
    return this.http.get<CertificateTemplate[]>(`${this.base}/certificate-templates`);
  }

  listForStudent(studentId: number): Observable<GeneratedCertificate[]> {
    return this.http.get<GeneratedCertificate[]>(`${this.base}/students/${studentId}/certificates`);
  }

  generate(body: {
    studentId: number;
    templateCode: string;
    enrollmentId?: number | null;
  }): Observable<GeneratedCertificate> {
    return this.http.post<GeneratedCertificate>(`${this.base}/certificates/generate`, body);
  }

  download(id: number): Observable<CertificateDownload> {
    return this.http.get<CertificateDownload>(`${this.base}/certificates/${id}/download`);
  }
}
