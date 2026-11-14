import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import flatpickr from 'flatpickr';
import type { Instance as FlatpickrInstance } from 'flatpickr/dist/types/instance';
import type { BaseOptions } from 'flatpickr/dist/types/options';

export type UiDatepickerMode = 'date' | 'time' | 'datetime';

@Component({
  selector: 'app-ui-datepicker',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiDatepicker),
      multi: true,
    },
  ],
  template: `
    <label class="field" [attr.for]="inputId">
      <span class="field__label">{{ label }}</span>
      <span class="field__control-wrap">
        <input
          #inputEl
          class="field__control"
          type="text"
          [id]="inputId"
          [disabled]="disabled"
          [attr.placeholder]="resolvedPlaceholder"
          [attr.autocomplete]="'off'"
          (blur)="onTouched()"
        />
        <button
          type="button"
          class="field__icon"
          tabindex="-1"
          [disabled]="disabled"
          [attr.aria-label]="mode === 'time' ? 'Open time picker' : 'Open date picker'"
          (click)="open()"
        >
          <i
            class="fa-solid"
            [class.fa-calendar-days]="mode !== 'time'"
            [class.fa-clock]="mode === 'time'"
            aria-hidden="true"
          ></i>
        </button>
      </span>
      @if (hint) {
        <span class="field__hint">{{ hint }}</span>
      }
    </label>
  `,
  styles: `
    .field {
      display: grid;
      gap: 0.35rem;
    }
    .field__label {
      font-size: 0.875rem;
      font-weight: 700;
    }
    .field__control-wrap {
      position: relative;
      display: block;
    }
    .field__control {
      width: 100%;
      box-sizing: border-box;
      min-height: 44px;
      border: 1px solid var(--ims-border);
      border-radius: 8px;
      padding: 0.55rem 2.5rem 0.55rem 0.75rem;
      font: inherit;
      color: var(--ims-text);
      background: #fff;
    }
    .field__control:focus-visible {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 2px;
    }
    .field__icon {
      position: absolute;
      right: 0.35rem;
      top: 50%;
      transform: translateY(-50%);
      width: 2rem;
      height: 2rem;
      border: 0;
      border-radius: 6px;
      background: transparent;
      color: var(--ims-text-muted);
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
    }
    .field__icon:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .field__hint {
      font-size: 0.8rem;
      color: var(--ims-text-muted);
    }
  `,
})
export class UiDatepicker implements AfterViewInit, OnChanges, OnDestroy, ControlValueAccessor {
  @ViewChild('inputEl', { static: true }) inputEl!: ElementRef<HTMLInputElement>;

  @Input({ required: true }) label!: string;
  @Input({ required: true }) inputId!: string;
  /** date → YYYY-MM-DD, time → HH:mm, datetime → YYYY-MM-DDTHH:mm */
  @Input() mode: UiDatepickerMode = 'date';
  @Input() min = '';
  @Input() max = '';
  @Input() hint = '';
  @Input() placeholder = '';

  disabled = false;
  private value = '';
  private picker: FlatpickrInstance | null = null;
  private onChange: (value: string) => void = () => undefined;
  onTouched: () => void = () => undefined;

  ngAfterViewInit(): void {
    this.initPicker();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.picker) {
      return;
    }
    if (changes['mode'] && !changes['mode'].firstChange) {
      this.picker.destroy();
      this.picker = null;
      this.initPicker();
      return;
    }
    if (changes['min'] || changes['max']) {
      this.picker.set('minDate', this.min || undefined);
      this.picker.set('maxDate', this.max || undefined);
    }
  }

  ngOnDestroy(): void {
    this.picker?.destroy();
    this.picker = null;
  }

  writeValue(value: string | null): void {
    this.value = value ?? '';
    if (this.picker) {
      this.picker.setDate(this.value || '', false);
    } else if (this.inputEl?.nativeElement) {
      this.inputEl.nativeElement.value = this.formatDisplay(this.value);
    }
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.picker) {
      if (isDisabled) {
        this.picker._input.setAttribute('disabled', 'disabled');
      } else {
        this.picker._input.removeAttribute('disabled');
      }
    }
  }

  get resolvedPlaceholder(): string {
    if (this.placeholder) {
      return this.placeholder;
    }
    if (this.mode === 'time') {
      return 'HH:mm';
    }
    if (this.mode === 'datetime') {
      return 'YYYY-MM-DD HH:mm';
    }
    return 'YYYY-MM-DD';
  }

  open(): void {
    if (!this.disabled) {
      this.picker?.open();
    }
  }

  private initPicker(): void {
    const options: Partial<BaseOptions> = {
      allowInput: false,
      disableMobile: true,
      clickOpens: true,
      dateFormat: this.dateFormat(),
      enableTime: this.mode !== 'date',
      noCalendar: this.mode === 'time',
      time_24hr: true,
      minuteIncrement: 5,
      defaultDate: this.value || undefined,
      minDate: this.min || undefined,
      maxDate: this.max || undefined,
      onChange: (_selected, dateStr) => {
        const next = this.toModelValue(dateStr);
        this.value = next;
        this.onChange(next);
      },
      onClose: () => this.onTouched(),
    };

    this.picker = flatpickr(this.inputEl.nativeElement, options);
    if (this.disabled) {
      this.setDisabledState(true);
    }
  }

  private dateFormat(): string {
    if (this.mode === 'time') {
      return 'H:i';
    }
    if (this.mode === 'datetime') {
      return 'Y-m-d H:i';
    }
    return 'Y-m-d';
  }

  /** Keep API/form values as HTML5-compatible strings. */
  private toModelValue(dateStr: string): string {
    if (!dateStr) {
      return '';
    }
    if (this.mode === 'datetime') {
      return dateStr.replace(' ', 'T');
    }
    return dateStr;
  }

  private formatDisplay(value: string): string {
    if (!value) {
      return '';
    }
    if (this.mode === 'datetime') {
      return value.replace('T', ' ');
    }
    return value;
  }
}
