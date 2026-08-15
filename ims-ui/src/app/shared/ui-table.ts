import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-table',
  template: `
    <div class="table-wrap" role="region" [attr.aria-label]="caption || 'Data table'" tabindex="0">
      <table class="table">
        @if (caption) {
          <caption class="sr-only">{{ caption }}</caption>
        }
        <thead>
          <tr>
            <ng-content select="[table-headers]" />
          </tr>
        </thead>
        <tbody>
          <ng-content />
        </tbody>
      </table>
    </div>
  `,
  styles: `
    .table-wrap {
      overflow-x: auto;
      border: 1px solid var(--ims-border);
      border-radius: var(--ims-radius);
      background: var(--ims-card-bg);
    }
    .table {
      width: 100%;
      border-collapse: collapse;
      min-width: 480px;
    }
    :host ::ng-deep th,
    :host ::ng-deep td {
      text-align: left;
      padding: 0.75rem 1rem;
      border-bottom: 1px solid var(--ims-border);
      font-size: 0.925rem;
    }
    :host ::ng-deep th {
      font-weight: 700;
      color: var(--ims-text-muted);
      background: #fafbfe;
    }
    .sr-only {
      position: absolute;
      width: 1px;
      height: 1px;
      padding: 0;
      margin: -1px;
      overflow: hidden;
      clip: rect(0, 0, 0, 0);
      border: 0;
    }
  `,
})
export class UiTable {
  @Input() caption = '';
}
