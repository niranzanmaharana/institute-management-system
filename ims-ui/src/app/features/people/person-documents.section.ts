import { Component, Input, OnChanges, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage } from '../../core/http-error';
import { EmptyState } from '../../shared/empty-state';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { UiButton } from '../../shared/ui-button';
import { UiFileDropzone } from '../../shared/ui-file-dropzone';
import { UiSelect, UiSelectOption } from '../../shared/ui-select';
import {
  PersonDocument,
  PersonDocumentService,
  PersonOwnerType,
  normalizeContentType,
} from './person-document.service';

@Component({
  selector: 'app-person-documents',
  imports: [
    FormsModule,
    UiButton,
    UiSelect,
    UiFileDropzone,
    LoadingState,
    ErrorState,
    EmptyState,
    StatusBadge,
  ],
  templateUrl: './person-documents.section.html',
  styleUrl: './person-documents.section.scss',
})
export class PersonDocumentsSection implements OnChanges {
  private readonly api = inject(PersonDocumentService);
  private readonly toasts = inject(ToastService);
  readonly auth = inject(AuthService);

  @Input({ required: true }) ownerType!: PersonOwnerType;
  @Input({ required: true }) ownerId!: number;

  readonly maxBytes = 5 * 1024 * 1024;
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly rows = signal<PersonDocument[]>([]);
  readonly selectedFile = signal<File | null>(null);
  docType = 'ID_PROOF';

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
    this.loadError.set(null);
    this.api.list(this.ownerType, this.ownerId).subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.loading.set(false);
      },
      error: (err) => {
        this.loadError.set(httpErrorMessage(err, 'Could not load documents.'));
        this.loading.set(false);
      },
    });
  }

  onRejected(message: string): void {
    this.toasts.warning(message);
  }

  async upload(): Promise<void> {
    const file = this.selectedFile();
    if (!file || this.uploading() || !this.canWrite) {
      return;
    }
    if (file.size > this.maxBytes) {
      this.toasts.warning('File exceeds the maximum size of 5 MB.');
      return;
    }
    const contentType = normalizeContentType(file, this.docType);
    if (!contentType) {
      this.toasts.warning('Allowed types are PDF, JPEG, and PNG.');
      return;
    }

    this.uploading.set(true);
    try {
      await this.api.uploadFile(this.ownerType, this.ownerId, file, this.docType);
      this.selectedFile.set(null);
      this.toasts.success('Document uploaded.');
    } catch (err) {
      this.toasts.danger(this.uploadError(err));
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
        this.toasts.danger(httpErrorMessage(err, 'Could not download document.'));
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
      next: () => {
        this.api.notifyChanged();
        this.toasts.success('Document deleted.');
      },
      error: (err) => {
        this.toasts.danger(httpErrorMessage(err, 'Could not delete document.'));
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
