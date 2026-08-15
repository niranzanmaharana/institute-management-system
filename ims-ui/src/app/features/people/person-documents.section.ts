import { Component, Input, OnChanges, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage } from '../../core/http-error';
import { EmptyState } from '../../shared/empty-state';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { StatusBadge } from '../../shared/status-badge';
import { UiButton } from '../../shared/ui-button';
import { UiSelect, UiSelectOption } from '../../shared/ui-select';
import {
  PersonDocument,
  PersonDocumentService,
  PersonOwnerType,
  normalizeContentType,
} from './person-document.service';

const MAX_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'app-person-documents',
  imports: [FormsModule, UiButton, UiSelect, LoadingState, ErrorState, EmptyState, StatusBadge],
  templateUrl: './person-documents.section.html',
  styleUrl: './person-documents.section.scss',
})
export class PersonDocumentsSection implements OnChanges {
  private readonly api = inject(PersonDocumentService);
  readonly auth = inject(AuthService);

  @Input({ required: true }) ownerType!: PersonOwnerType;
  @Input({ required: true }) ownerId!: number;

  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);
  readonly rows = signal<PersonDocument[]>([]);
  docType = 'ID_PROOF';
  selectedFile: File | null = null;

  readonly docTypeOptions: UiSelectOption[] = [
    { value: 'ID_PROOF', label: 'ID proof' },
    { value: 'ADDRESS_PROOF', label: 'Address proof' },
    { value: 'PHOTO', label: 'Photo' },
    { value: 'QUALIFICATION', label: 'Qualification' },
    { value: 'OTHER', label: 'Other' },
  ];

  get canWrite(): boolean {
    return this.auth.hasPermission('document:write') || this.auth.hasRole('ADMIN');
  }

  ngOnChanges(): void {
    if (this.ownerId) {
      this.load();
    }
  }

  constructor() {
    effect(() => {
      this.api.catalogRevision();
      untracked(() => {
        if (this.ownerId) {
          this.load();
        }
      });
    });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.ownerType, this.ownerId).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not load documents.'));
        this.loading.set(false);
      },
    });
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  async upload(): Promise<void> {
    const file = this.selectedFile;
    if (!file || this.uploading() || !this.canWrite) {
      return;
    }
    if (file.size > MAX_BYTES) {
      this.error.set('File exceeds the maximum size of 5 MB.');
      return;
    }
    const contentType = normalizeContentType(file, this.docType);
    if (!contentType) {
      this.error.set('Allowed types are PDF, JPEG, and PNG.');
      return;
    }

    this.uploading.set(true);
    this.error.set(null);
    try {
      await this.api.uploadFile(this.ownerType, this.ownerId, file, this.docType);
      this.selectedFile = null;
    } catch (err) {
      this.error.set(this.uploadError(err));
    } finally {
      this.uploading.set(false);
    }
  }

  download(row: PersonDocument): void {
    this.api.download(this.ownerType, this.ownerId, row.id).subscribe({
      next: (res) => {
        window.open(res.downloadUrl, '_blank', 'noopener');
      },
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not download document.'));
      },
    });
  }

  remove(row: PersonDocument): void {
    if (!this.canWrite) {
      return;
    }
    if (!confirm(`Delete ${row.fileName}?`)) {
      return;
    }
    this.api.delete(this.ownerType, this.ownerId, row.id).subscribe({
      next: () => this.api.notifyChanged(),
      error: (err) => {
        this.error.set(httpErrorMessage(err, 'Could not delete document.'));
      },
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  labelForType(docType: string): string {
    return this.docTypeOptions.find((o) => o.value === docType)?.label ?? docType;
  }

  private uploadError(err: unknown): string {
    if (err instanceof TypeError) {
      return 'Could not reach object storage. Start MinIO (infra docker compose) and retry.';
    }
    if (err instanceof Error && err.message) {
      return err.message;
    }
    return httpErrorMessage(err, 'Could not upload document.');
  }
}
