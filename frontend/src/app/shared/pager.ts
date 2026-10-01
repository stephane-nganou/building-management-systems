import {
  ChangeDetectionStrategy,
  Component,
  EffectRef,
  WritableSignal,
  effect,
  input,
  output,
} from '@angular/core';

import { Page, PageInfo } from '../core/models';
import { TranslatePipe } from './translate.pipe';

/**
 * Steps back a page when the one on screen has emptied, as it does once its
 * last row is deleted, so the list never shows an empty table between pages.
 */
export function stepBackFromEmptyPage(
  page: WritableSignal<number>,
  list: () => Page<unknown> | undefined,
): EffectRef {
  return effect(() => {
    const current = list();
    if (current && current.content.length === 0 && current.page.number > 0) {
      page.set(current.page.number - 1);
    }
  });
}

/** Previous and next under a list the server hands out a page at a time. Hidden for a single page. */
@Component({
  selector: 'bms-pager',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (page().totalPages > 1) {
      <nav class="pager" [attr.aria-label]="'pager.label' | t">
        <button class="quiet" type="button" [disabled]="page().number === 0" (click)="go.emit(page().number - 1)">
          {{ 'pager.previous' | t }}
        </button>
        <span class="muted">
          {{ 'pager.position' | t: { page: page().number + 1, pages: page().totalPages } }}
        </span>
        <button
          class="quiet"
          type="button"
          [disabled]="page().number + 1 >= page().totalPages"
          (click)="go.emit(page().number + 1)"
        >
          {{ 'pager.next' | t }}
        </button>
      </nav>
    }
  `,
})
export class Pager {
  readonly page = input.required<PageInfo>();
  readonly go = output<number>();
}
