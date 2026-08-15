import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EmptyState } from '../../shared/empty-state';
import { ErrorState } from '../../shared/error-state';
import { LoadingState } from '../../shared/loading-state';
import { ResponsiveDataView } from '../../shared/responsive-data-view';
import { StatusBadge } from '../../shared/status-badge';
import { ToastService } from '../../shared/toast.service';
import { UiButton } from '../../shared/ui-button';
import { UiCard } from '../../shared/ui-card';
import { UiDatepicker } from '../../shared/ui-datepicker';
import { UiInput } from '../../shared/ui-input';
import { UiModal } from '../../shared/ui-modal';
import { UiPagination } from '../../shared/ui-pagination';
import { UiSelect } from '../../shared/ui-select';
import { UiTable } from '../../shared/ui-table';

@Component({
  selector: 'app-design-system-page',
  imports: [
    FormsModule,
    UiCard,
    UiButton,
    UiInput,
    UiSelect,
    UiDatepicker,
    UiModal,
    UiTable,
    UiPagination,
    EmptyState,
    ErrorState,
    LoadingState,
    StatusBadge,
    ResponsiveDataView,
  ],
  templateUrl: './design-system.page.html',
  styleUrl: './design-system.page.scss',
})
export class DesignSystemPage {
  private readonly toasts = inject(ToastService);

  sampleText = '';
  sampleSelect = 'a';
  sampleDate = '2026-08-09';
  modalOpen = signal(false);
  page = signal(1);

  readonly selectOptions = [
    { value: 'a', label: 'Option A' },
    { value: 'b', label: 'Option B' },
  ];

  readonly rows = [
    { id: '1', title: 'Ada Lovelace', subtitle: 'Batch A1', meta: 'Active' },
    { id: '2', title: 'Alan Turing', subtitle: 'Batch B2', meta: 'Pending' },
  ];

  openModal(): void {
    this.modalOpen.set(true);
  }

  closeModal(): void {
    this.modalOpen.set(false);
  }

  showToast(): void {
    this.toasts.show('Design system toast works', 'success');
  }
}
