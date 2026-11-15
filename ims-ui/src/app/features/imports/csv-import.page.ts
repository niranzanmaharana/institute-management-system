import { Component, OnInit, inject, signal } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage } from '../../core/http-error';
import { EmptyState } from '../../shared/empty-state';
import { ErrorState } from '../../shared/error-state';
import { ToastService } from '../../shared/toast.service';
import { UiButton } from '../../shared/ui-button';
import { UiCard } from '../../shared/ui-card';
import { UiFileDropzone } from '../../shared/ui-file-dropzone';
import { ImportResult, ImportService } from './import.service';

type ImportKind = 'students' | 'faculties';

@Component({
  selector: 'app-csv-import-page',
  imports: [UiCard, UiButton, UiFileDropzone, EmptyState, ErrorState],
  templateUrl: './csv-import.page.html',
  styleUrl: './csv-import.page.scss',
})
export class CsvImportPage implements OnInit {
  private readonly importApi = inject(ImportService);
  private readonly toasts = inject(ToastService);
  readonly auth = inject(AuthService);

  readonly maxBytes = 2 * 1024 * 1024;
  readonly kind = signal<ImportKind>('students');
  readonly uploading = signal(false);
  readonly result = signal<ImportResult | null>(null);
  readonly selectedFile = signal<File | null>(null);

  get canStudents(): boolean {
    return this.auth.hasPermission('import:write') && this.auth.hasPermission('student:write');
  }

  get canFaculties(): boolean {
    return this.auth.hasPermission('import:write') && this.auth.hasPermission('faculty:write');
  }

  get canAny(): boolean {
    return this.canStudents || this.canFaculties;
  }

  ngOnInit(): void {
    if (!this.canStudents && this.canFaculties) {
      this.kind.set('faculties');
    }
  }

  setKind(kind: ImportKind): void {
    this.kind.set(kind);
    this.selectedFile.set(null);
    this.result.set(null);
  }

  onRejected(message: string): void {
    this.toasts.warning(message);
  }

  downloadTemplate(): void {
    const kind = this.kind();
    const req =
      kind === 'students'
        ? this.importApi.downloadStudentTemplate()
        : this.importApi.downloadFacultyTemplate();
    req.subscribe({
      next: (blob) =>
        this.saveBlob(
          blob,
          kind === 'students' ? 'students-import-template.csv' : 'faculties-import-template.csv',
        ),
      error: (err) =>
        this.toasts.danger(httpErrorMessage(err, 'Could not download template.')),
    });
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file || this.uploading()) {
      return;
    }
    const kind = this.kind();
    if (kind === 'students' && !this.canStudents) {
      return;
    }
    if (kind === 'faculties' && !this.canFaculties) {
      return;
    }
    this.uploading.set(true);
    this.result.set(null);
    const req =
      kind === 'students'
        ? this.importApi.importStudents(file)
        : this.importApi.importFaculties(file);
    req.subscribe({
      next: (res) => {
        this.uploading.set(false);
        this.result.set(res);
        this.selectedFile.set(null);
        if (res.failed === 0) {
          this.toasts.success(`Imported ${res.inserted} row(s) successfully.`);
        } else {
          this.toasts.warning(
            `Imported ${res.inserted} row(s); ${res.failed} failed. See error report below.`,
            5000,
          );
        }
      },
      error: (err) => {
        this.uploading.set(false);
        this.toasts.danger(httpErrorMessage(err, 'Import failed.'));
      },
    });
  }

  downloadErrorReport(): void {
    const res = this.result();
    if (!res?.errorReportCsv) {
      return;
    }
    const blob = new Blob([res.errorReportCsv], { type: 'text/csv;charset=utf-8' });
    this.saveBlob(blob, `${res.entityType.toLowerCase()}-import-errors.csv`);
  }

  private saveBlob(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(url);
  }
}
