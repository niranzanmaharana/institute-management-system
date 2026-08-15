import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-ui-card',
  template: `
    <section class="card" [class.card--flush]="flush">
      @if (title) {
        <header class="card__header">
          <h2>{{ title }}</h2>
          <ng-content select="[cardActions]" />
        </header>
      }
      <div class="card__body">
        <ng-content />
      </div>
    </section>
  `,
  styles: `
    .card {
      background: var(--ims-card-bg);
      border-radius: var(--ims-radius);
      box-shadow: var(--ims-shadow);
      border: 1px solid var(--ims-border);
    }
    .card__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 1.25rem 0;
    }
    .card__header h2 {
      margin: 0;
      font-size: 1.05rem;
      font-weight: 700;
    }
    .card__body {
      padding: 1.25rem;
    }
    .card--flush .card__body {
      padding: 0;
    }
  `,
})
export class UiCard {
  @Input() title = '';
  @Input() flush = false;
}
