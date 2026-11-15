import { Component, ElementRef, Input, ViewChild, model, output, signal } from '@angular/core';

@Component({
  selector: 'app-ui-file-dropzone',
  templateUrl: './ui-file-dropzone.html',
  styleUrl: './ui-file-dropzone.scss',
})
export class UiFileDropzone {
  /** Selected file (two-way). Parent clears with `file.set(null)` / `[(file)]`. */
  readonly file = model<File | null>(null);
  readonly dragOver = signal(false);

  @Input() label = 'File';
  @Input() accept = '';
  /** Footer hint, e.g. "Accepted file types: PDF, JPEG, PNG". */
  @Input() acceptedHint = '';
  @Input() showSecure = true;
  @Input() disabled = false;
  /** Shown after size when a file is selected, e.g. "CSV ready to import". */
  @Input() readyMeta = 'Ready to upload';
  @Input() emptyTitle = 'Drag and drop here';
  @Input() browseLabel = 'Browse files';
  @Input() ariaLabel = 'Choose file';
  /** Compact layout for tight spaces (e.g. profile photo). */
  @Input() compact = false;
  /** Optional max size; rejected files emit `rejected` and are not kept. */
  @Input() maxBytes: number | null = null;

  readonly rejected = output<string>();

  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  private dragDepth = 0;

  formatSize(bytes: number): string {
    if (bytes < 1024) {
      return `${bytes} B`;
    }
    if (bytes < 1024 * 1024) {
      return `${(bytes / 1024).toFixed(1)} KB`;
    }
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }

  openPicker(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    if (this.disabled) {
      return;
    }
    this.fileInput?.nativeElement.click();
  }

  clear(event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.file.set(null);
    this.dragOver.set(false);
    this.dragDepth = 0;
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const next = input.files && input.files.length > 0 ? input.files[0] : null;
    this.applyFile(next);
    input.value = '';
  }

  onDragEnter(event: DragEvent): void {
    if (this.disabled) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    this.dragDepth++;
    this.dragOver.set(true);
  }

  onDragOver(event: DragEvent): void {
    if (this.disabled) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'copy';
    }
    this.dragOver.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragDepth = Math.max(0, this.dragDepth - 1);
    if (this.dragDepth === 0) {
      this.dragOver.set(false);
    }
  }

  onDrop(event: DragEvent): void {
    if (this.disabled) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    this.dragDepth = 0;
    this.dragOver.set(false);
    const next = event.dataTransfer?.files?.[0] ?? null;
    this.applyFile(next);
  }

  private applyFile(next: File | null): void {
    if (!next) {
      this.file.set(null);
      return;
    }
    if (!this.matchesAccept(next)) {
      this.file.set(null);
      this.rejected.emit(this.acceptRejectMessage());
      return;
    }
    if (this.maxBytes != null && next.size > this.maxBytes) {
      this.file.set(null);
      this.rejected.emit(`File exceeds the maximum size of ${this.formatSize(this.maxBytes)}.`);
      return;
    }
    this.file.set(next);
  }

  private matchesAccept(file: File): boolean {
    const accept = (this.accept || '').trim();
    if (!accept) {
      return true;
    }
    const tokens = accept.split(',').map((t) => t.trim().toLowerCase()).filter(Boolean);
    if (tokens.length === 0) {
      return true;
    }
    const name = file.name.toLowerCase();
    const type = (file.type || '').toLowerCase();
    return tokens.some((token) => {
      if (token.startsWith('.')) {
        return name.endsWith(token);
      }
      if (token.endsWith('/*')) {
        const prefix = token.slice(0, -1);
        return type.startsWith(prefix);
      }
      return type === token || name.endsWith(`.${token}`);
    });
  }

  private acceptRejectMessage(): string {
    if (this.acceptedHint) {
      return this.acceptedHint.replace(/^Accepted file types:\s*/i, 'Please choose a valid file. ');
    }
    return 'Please choose a valid file type.';
  }
}
