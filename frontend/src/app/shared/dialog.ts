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
import { IconButton } from './icon-button';
import { TranslatePipe } from './translate.pipe';

let nextId = 0;

/**
 * A modal built on the native dialog element, which already traps focus,
 * closes on Escape and hands focus back to whatever opened it. It opens as it
 * is rendered, so a page shows one with a plain @if.
 */
@Component({
  selector: 'bms-dialog',
  imports: [Icon, IconButton, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <dialog
      #dialog
      class="panel"
      [class.narrow]="narrow()"
      [attr.aria-labelledby]="headingId"
      (close)="closed.emit()"
      (mousedown)="pressedOnBackdrop = $event.target === dialog"
      (click)="closeOnBackdrop($event)"
    >
      <header>
        <h2 [id]="headingId">{{ heading() }}</h2>
        <button bmsIconButton icon="close" [label]="'common.close' | t" (click)="dialog.close()"></button>
      </header>
      <div class="body">
        <ng-content />
        @if (error()) {
          <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
        }
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
  /** What went wrong with the last attempt, shown where the user is looking. */
  readonly error = input<string | null>(null);
  readonly closed = output<void>();

  protected readonly headingId = `dialog-heading-${nextId++}`;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');
  protected pressedOnBackdrop = false;

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

  /**
   * The dialog fills its own box, so a click on the element itself landed on
   * the backdrop. Both ends of it must have: a drag that starts in a field and
   * ends outside is also delivered to the dialog, and must not throw the form
   * away.
   */
  protected closeOnBackdrop(event: MouseEvent): void {
    if (this.pressedOnBackdrop && event.target === this.dialog().nativeElement) {
      this.dialog().nativeElement.close();
    }
  }

  /** Closing, rather than just removing it, is what returns focus to the opener. */
  ngOnDestroy(): void {
    this.dialog().nativeElement.close();
  }
}
