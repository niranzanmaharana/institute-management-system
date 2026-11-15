import { Component, ElementRef, ViewChild, input, output } from '@angular/core';

@Component({
  selector: 'app-institute-logo',
  templateUrl: './institute-logo.component.html',
  styleUrl: './institute-logo.component.scss',
})
export class InstituteLogoComponent {
  /** Current display URL (http or object URL). */
  readonly previewUrl = input<string | null>(null);
  readonly name = input('');
  readonly disabled = input(false);
  readonly uploading = input(false);
  /** When false, logo is display-only (no camera / picker). */
  readonly editable = input(true);

  readonly fileSelected = output<File>();

  @ViewChild('fileInput') private fileInput?: ElementRef<HTMLInputElement>;

  get initials(): string {
    const raw = (this.name() || '').trim();
    if (!raw) {
      return 'I';
    }
    const parts = raw.split(/\s+/).filter(Boolean);
    if (parts.length === 1) {
      return parts[0].slice(0, 2).toUpperCase();
    }
    return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
  }

  openPicker(): void {
    if (!this.editable() || this.disabled() || this.uploading()) {
      return;
    }
    this.fileInput?.nativeElement.click();
  }

  onFileChange(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const file = inputEl.files?.[0] ?? null;
    inputEl.value = '';
    if (!file || !this.editable() || this.disabled() || this.uploading()) {
      return;
    }
    this.fileSelected.emit(file);
  }
}
