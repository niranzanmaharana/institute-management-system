import { Component, Input } from '@angular/core';
import { UiCard } from './ui-card';

export interface ResponsiveRow {
  id: string;
  title: string;
  subtitle?: string;
  meta?: string;
}

/** Table on wide screens; stacked cards on narrow (FR-UX-04). */
@Component({
  selector: 'app-responsive-data-view',
  imports: [UiCard],
  template: `
    <div class="rdv">
      <div class="rdv__table" role="table" [attr.aria-label]="caption">
        <div class="rdv__head" role="row">
          <span role="columnheader">Name</span>
          <span role="columnheader">Details</span>
          <span role="columnheader">Meta</span>
        </div>
        @for (row of rows; track row.id) {
          <div class="rdv__row" role="row">
            <span role="cell">{{ row.title }}</span>
            <span role="cell">{{ row.subtitle || '—' }}</span>
            <span role="cell">{{ row.meta || '—' }}</span>
          </div>
        }
      </div>

      <div class="rdv__cards">
        @for (row of rows; track row.id) {
          <app-ui-card>
            <strong>{{ row.title }}</strong>
            @if (row.subtitle) {
              <p>{{ row.subtitle }}</p>
            }
            @if (row.meta) {
              <p class="muted">{{ row.meta }}</p>
            }
          </app-ui-card>
        }
      </div>
    </div>
  `,
  styles: `
    .rdv__cards {
      display: none;
      gap: 0.75rem;
    }
    .rdv__table {
      display: grid;
      border: 1px solid var(--ims-border);
      border-radius: var(--ims-radius);
      overflow: hidden;
      background: #fff;
    }
    .rdv__head,
    .rdv__row {
      display: grid;
      grid-template-columns: 1.2fr 1.4fr 0.8fr;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid var(--ims-border);
    }
    .rdv__head {
      font-weight: 700;
      color: var(--ims-text-muted);
      background: #fafbfe;
    }
    .rdv__row:last-child {
      border-bottom: 0;
    }
    .muted {
      color: var(--ims-text-muted);
      margin: 0.25rem 0 0;
    }
    @media (max-width: 767px) {
      .rdv__table {
        display: none;
      }
      .rdv__cards {
        display: grid;
      }
    }
  `,
})
export class ResponsiveDataView {
  @Input() caption = 'Records';
  @Input() rows: ResponsiveRow[] = [];
}
