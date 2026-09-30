import { ChangeDetectionStrategy, Component, Injectable, inject, signal } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { Dialog } from './dialog';
import { TranslatePipe } from './translate.pipe';

export interface Confirmation {
  heading: string;
  body: string;
  action: string;
}

interface Pending extends Confirmation {
  answer: (yes: boolean) => void;
}

/** Asks before something that cannot be undone. */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private i18n = inject(TranslationService);

  readonly pending = signal<Pending | null>(null);

  ask(confirmation: Confirmation): Promise<boolean> {
    return new Promise((answer) => this.pending.set({ ...confirmation, answer }));
  }

  /** The question every delete button asks. */
  delete(name: string): Promise<boolean> {
    return this.ask({
      heading: this.i18n.translate('confirm.deleteTitle', { name }),
      body: this.i18n.translate('confirm.deleteBody'),
      action: this.i18n.translate('common.delete'),
    });
  }

  settle(yes: boolean): void {
    this.pending()?.answer(yes);
    this.pending.set(null);
  }
}

@Component({
  selector: 'bms-confirm',
  imports: [Dialog, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (service.pending(); as pending) {
      <bms-dialog [heading]="pending.heading" [narrow]="true" (closed)="service.settle(false)">
        <p>{{ pending.body }}</p>
        <ng-container actions>
          <button type="button" (click)="service.settle(false)">{{ 'common.cancel' | t }}</button>
          <button class="destructive" type="button" (click)="service.settle(true)">{{ pending.action }}</button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class ConfirmHost {
  protected readonly service = inject(ConfirmService);
}
