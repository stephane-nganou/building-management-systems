import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  afterNextRender,
  input,
  output,
  viewChild,
} from '@angular/core';

import { Icon } from './icon';
import { TranslatePipe } from './translate.pipe';

let nextId = 0;

/**
 * A modal built on the native dialog element, which already traps focus,
 * closes on Escape and hands focus back to whatever opened it. It opens as it
 * is rendered, so a page shows one with a plain @if.
 */
@Component({
  selector: 'bms-dialog',
  imports: [Icon, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialog
      class="panel"
      [class.narrow]="narrow()"
      [attr.aria-labelledby]="headingId"
      (close)="closed.emit()"
      (click)="closeOnBackdrop($event)"
    >
      <header>
        <h2 [id]="headingId">{{ heading() }}</h2>
        <button class="icon" type="button" [attr.aria-label]="'common.close' | t" (click)="dialog.close()">
          <bms-icon name="close" />
        </button>
      </header>
      <div class="body">
        <ng-content />
      </div>
      <footer>
        <ng-content select="[actions]" />
      </footer>
    </dialog>
  `,
})
export class Dialog implements OnDestroy {
  readonly heading = input.required<string>();
  readonly narrow = input(false);
  readonly closed = output<void>();

  protected readonly headingId = `dialog-heading-${nextId++}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      dialog.showModal();
      // The browser would focus the close button, which is the first control.
      // The first field is where the user actually starts, and in a question
      // with no fields the first answer is the safe one.
      dialog.querySelector<HTMLElement>('.body :is(input, select, textarea), footer button')?.focus();
    });
  }

  /** The dialog fills its own box, so a click on the element itself landed on the backdrop. */
  protected closeOnBackdrop(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.dialog().nativeElement.close();
    }
  }

  /** Closing, rather than just removing it, is what returns focus to the opener. */
  ngOnDestroy(): void {
    this.dialog().nativeElement.close();
  }
}
