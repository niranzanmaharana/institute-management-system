import { Component, Input, OnChanges, effect, inject, signal, untracked } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import { httpErrorMessage } from '../../core/http-error';
import { PersonDocumentService, PersonOwnerType } from './person-document.service';

@Component({
  selector: 'app-person-photo',
  templateUrl: './person-photo.component.html',
  styleUrl: './person-photo.component.scss',
})
export class PersonPhotoComponent implements OnChanges {
  private readonly api = inject(PersonDocumentService);
  private readonly auth = inject(AuthService);

  @Input({ required: true }) ownerType!: PersonOwnerType;
  @Input({ required: true }) ownerId!: number;
  @Input() firstName = '';
  @Input() lastName = '';

  readonly url = signal<string | null>(null);
  readonly uploading = signal(false);
  readonly error = signal<string | null>(null);

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
    this.error.set(null);
    this.api.photo(this.ownerType, this.ownerId).subscribe({
      next: (res) => this.url.set(res.downloadUrl),
      error: (err) => {
        this.url.set(null);
        if (err instanceof HttpErrorResponse && err.status === 404) {
          return;
        }
        this.error.set(httpErrorMessage(err, 'Could not load photo.'));
      },
    });
  }

  async onFile(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file || !this.canWrite || this.uploading()) {
      return;
    }
    this.uploading.set(true);
    this.error.set(null);
    try {
      await this.api.uploadFile(this.ownerType, this.ownerId, file, 'PHOTO');
    } catch (err) {
      this.error.set(uploadError(err));
    } finally {
      this.uploading.set(false);
    }
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
