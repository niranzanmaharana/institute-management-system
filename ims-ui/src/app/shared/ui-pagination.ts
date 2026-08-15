import { Component, EventEmitter, Input, Output } from '@angular/core';

@Component({
  selector: 'app-ui-pagination',
  template: `
    <nav class="pager" aria-label="Pagination">
      <button type="button" class="pager__btn" [disabled]="page <= 1" (click)="go(page - 1)">Previous</button>
      <span class="pager__status">Page {{ page }} of {{ totalPages }}</span>
      <button type="button" class="pager__btn" [disabled]="page >= totalPages" (click)="go(page + 1)">Next</button>
    </nav>
  `,
  styles: `
    .pager {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      gap: 0.75rem;
      justify-content: flex-end;
    }
    .pager__btn {
      min-height: 44px;
      padding: 0.5rem 0.9rem;
      border: 1px solid var(--ims-border);
      border-radius: 8px;
      background: #fff;
      cursor: pointer;
      font: inherit;
    }
    .pager__btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
    .pager__btn:focus-visible {
      outline: 2px solid var(--ims-primary-light);
      outline-offset: 2px;
    }
    .pager__status {
      color: var(--ims-text-muted);
      font-size: 0.9rem;
    }
  `,
})
export class UiPagination {
  @Input() page = 1;
  @Input() totalPages = 1;
  @Output() pageChange = new EventEmitter<number>();

  go(next: number): void {
    if (next < 1 || next > this.totalPages || next === this.page) {
      return;
    }
    this.pageChange.emit(next);
  }
}
