import {
  Component,
  ElementRef,
  Input,
  OnChanges,
  ViewChild,
  effect,
  inject,
  signal,
  untracked,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage } from '../../core/http-error';
import { ToastService } from '../../shared/toast.service';
import { PersonDocumentService, PersonOwnerType } from './person-document.service';

@Component({
  selector: 'app-person-photo',
  templateUrl: './person-photo.component.html',
  styleUrl: './person-photo.component.scss',
})
export class PersonPhotoComponent implements OnChanges {
  private readonly api = inject(PersonDocumentService);
  private readonly auth = inject(AuthService);
  private readonly toasts = inject(ToastService);

  @Input({ required: true }) ownerType!: PersonOwnerType;
  @Input({ required: true }) ownerId!: number;
  @Input() firstName = '';
  @Input() lastName = '';

  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  readonly maxBytes = 5 * 1024 * 1024;
  readonly url = signal<string | null>(null);
  readonly uploading = signal(false);

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

  get canWrite(): boolean {
    return this.auth.hasPermission('document:write') || this.auth.hasRole('ADMIN');
  }

  get initials(): string {
    const a = (this.firstName || '').trim().charAt(0);
    const b = (this.lastName || '').trim().charAt(0);
    const letters = `${a}${b}`.toUpperCase();
    return letters || '?';
  }

  get alt(): string {
    const name = `${this.firstName} ${this.lastName}`.trim();
    return name ? `${name} photo` : 'Profile photo';
  }

  ngOnChanges(): void {
    if (this.ownerId) {
      this.load();
    }
  }

  load(): void {
    this.api.photo(this.ownerType, this.ownerId).subscribe({
      next: (res) => this.url.set(res.downloadUrl),
      error: (err) => {
        this.url.set(null);
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return;
        }
        this.toasts.danger(httpErrorMessage(err, 'Could not load photo.'));
      },
    });
  }

  openPicker(): void {
    if (!this.canWrite || this.uploading()) {
      return;
    }
    this.fileInput?.nativeElement.click();
  }

  async onFileChange(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file || !this.canWrite || this.uploading()) {
      return;
    }
    if (!this.isImageFile(file)) {
      this.toasts.warning('Please choose a JPEG or PNG image.');
      return;
    }
    if (file.size > this.maxBytes) {
      this.toasts.warning('Photo exceeds the maximum size of 5 MB.');
      return;
    }

    this.uploading.set(true);
    try {
      await this.api.uploadFile(this.ownerType, this.ownerId, file, 'PHOTO');
      this.toasts.success('Photo updated.');
    } catch (err) {
      this.toasts.danger(uploadError(err));
    } finally {
      this.uploading.set(false);
    }
  }

  private isImageFile(file: File): boolean {
    const name = file.name.toLowerCase();
    const type = (file.type || '').toLowerCase();
    return (
      type === 'image/jpeg' ||
      type === 'image/png' ||
      name.endsWith('.jpg') ||
      name.endsWith('.jpeg') ||
      name.endsWith('.png')
    );
  }
}

function uploadError(err: unknown): string {
  if (err instanceof TypeError) {
    return 'Could not reach object storage. Start MinIO and retry.';
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return httpErrorMessage(err, 'Could not upload photo.');
}
