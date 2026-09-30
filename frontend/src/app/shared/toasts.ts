import { ChangeDetectionStrategy, Component, Injectable, inject, signal } from '@angular/core';

import { Icon } from './icon';

interface Toast {
  id: number;
  text: string;
}

const LIFETIME_MS = 3500;

/** Says, briefly, that something the user asked for has been done. */
@Injectable({ providedIn: 'root' })
export class ToastService {
  private next = 0;
  private readonly list = signal<Toast[]>([]);

  readonly toasts = this.list.asReadonly();

  show(text: string): void {
    const id = this.next++;
    this.list.update((toasts) => [...toasts, { id, text }]);
    setTimeout(() => this.list.update((toasts) => toasts.filter((toast) => toast.id !== id)), LIFETIME_MS);
  }
}

@Component({
  selector: 'bms-toasts',
  imports: [Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="toasts" role="status" aria-live="polite">
      @for (toast of service.toasts(); track toast.id) {
        <div class="toast">
          <bms-icon name="check" />
          {{ toast.text }}
        </div>
      }
    </div>
  `,
})
export class Toasts {
  protected readonly service = inject(ToastService);
}
