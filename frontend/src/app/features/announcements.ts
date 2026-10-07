import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { rxResource } from '@angular/core/rxjs-interop';

import { AdminApi } from '../core/api';
import { TranslationService } from '../core/i18n';
import { Announcement, AnnouncementKind, AnnouncementRequest } from '../core/models';
import { MessageKey } from '../i18n/en';
import { AnnouncementsFeed } from '../shared/announcements';
import { ConfirmService } from '../shared/confirm';
import { fromLocalInput, toLocalInput } from '../shared/dates';
import { Dialog } from '../shared/dialog';
import { Icon } from '../shared/icon';
import { IconButton } from '../shared/icon-button';
import { WhenPipe } from '../shared/money.pipe';
import { ToastService } from '../shared/toasts';
import { TranslatePipe } from '../shared/translate.pipe';

const DAY_MS = 86_400_000;

/** Where an announcement stands at `now`: not started, on screen, or over. */
export function phase(announcement: Announcement, now: number): 'scheduled' | 'showing' | 'ended' {
  if (now < Date.parse(announcement.startsAt)) {
    return 'scheduled';
  }
  return now < Date.parse(announcement.endsAt) ? 'showing' : 'ended';
}

/** A new one is suggested from now for a day. The dates are as the `datetime-local` fields hold them. */
function blank(): AnnouncementRequest {
  const now = Date.now();
  return {
    kind: 'INFO',
    messageEn: '',
    messageFr: '',
    messageDe: '',
    startsAt: toLocalInput(new Date(now).toISOString()),
    endsAt: toLocalInput(new Date(now + DAY_MS).toISOString()),
  };
}

/** The administrator's messages to everybody: what is showing, what is coming, and what is over. */
@Component({
  selector: 'bms-announcements-page',
  imports: [FormsModule, Dialog, Icon, IconButton, WhenPipe, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="band">
      <div class="band-head">
        <div>
          <h1>{{ 'announcements.title' | t }}</h1>
          <p>{{ 'announcements.subtitle' | t }}</p>
        </div>
        <button class="primary" type="button" (click)="open(null)">
          <bms-icon name="plus" />
          {{ 'announcements.add' | t }}
        </button>
      </div>

      @if (announcements.isLoading()) {
        <p class="loading">{{ 'announcements.loading' | t }}</p>
      } @else if (announcements.hasValue() && announcements.value()!.length === 0) {
        <p class="muted">{{ 'announcements.empty' | t }}</p>
      } @else if (announcements.hasValue()) {
        <div class="sheet-frame">
          <table class="sheet">
            <thead>
              <tr>
                <th>{{ 'announcements.kind' | t }}</th>
                <th>{{ 'announcements.message' | t }}</th>
                <th>{{ 'common.from' | t }}</th>
                <th>{{ 'common.to' | t }}</th>
                <th>{{ 'common.status' | t }}</th>
                <th>
                  <span class="visually-hidden">{{ 'common.actions' | t }}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              @for (announcement of announcements.value(); track announcement.id) {
                <tr>
                  <td>{{ kindLabel(announcement.kind) | t }}</td>
                  <td class="strong">{{ announcement.messageEn }}</td>
                  <td class="muted">{{ announcement.startsAt | when }}</td>
                  <td class="muted">{{ announcement.endsAt | when }}</td>
                  <td>
                    <span class="mark {{ phaseOf(announcement) }}">
                      {{ phaseLabel(announcement) | t }}
                    </span>
                  </td>
                  <td class="actions-cell">
                    <span class="row-actions">
                      <button
                        bmsIconButton
                        icon="edit"
                        [label]="'common.edit' | t"
                        (click)="open(announcement)"
                      ></button>
                      <button
                        bmsIconButton
                        class="danger"
                        icon="trash"
                        [label]="'common.delete' | t"
                        (click)="remove(announcement)"
                      ></button>
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }

      @if (error() && !editing()) {
        <p class="notice" role="alert"><bms-icon name="alert" />{{ error() }}</p>
      }
    </section>

    @if (editing()) {
      <bms-dialog
        [heading]="(editingId ? 'announcements.edit' : 'announcements.add') | t"
        [error]="error()"
        (closed)="editing.set(false)"
      >
        <div class="field">
          <label for="kind">{{ 'announcements.kind' | t }}</label>
          <select id="kind" [(ngModel)]="form.kind">
            @for (option of kinds; track option) {
              <option [value]="option">{{ kindLabel(option) | t }}</option>
            }
          </select>
        </div>
        <div class="field">
          <label for="messageEn">{{ 'announcements.messageEn' | t }}</label>
          <textarea
            id="messageEn"
            rows="3"
            maxlength="1000"
            [(ngModel)]="form.messageEn"
          ></textarea>
        </div>
        <div class="field">
          <label for="messageFr">{{ 'announcements.messageFr' | t }}</label>
          <textarea
            id="messageFr"
            rows="3"
            maxlength="1000"
            [(ngModel)]="form.messageFr"
          ></textarea>
        </div>
        <div class="field">
          <label for="messageDe">{{ 'announcements.messageDe' | t }}</label>
          <textarea
            id="messageDe"
            rows="3"
            maxlength="1000"
            [(ngModel)]="form.messageDe"
          ></textarea>
          <p class="hint">{{ 'announcements.fallbackHint' | t }}</p>
        </div>
        <div class="grid-2">
          <div class="field">
            <label for="startsAt">{{ 'announcements.startsAt' | t }}</label>
            <input id="startsAt" type="datetime-local" [(ngModel)]="form.startsAt" />
          </div>
          <div class="field">
            <label for="endsAt">{{ 'announcements.endsAt' | t }}</label>
            <input id="endsAt" type="datetime-local" [(ngModel)]="form.endsAt" />
          </div>
        </div>
        <ng-container actions>
          <button type="button" (click)="editing.set(false)">{{ 'common.cancel' | t }}</button>
          <button class="primary" type="button" [disabled]="!complete()" (click)="save()">
            {{ (editingId ? 'common.saveChanges' : 'announcements.publish') | t }}
          </button>
        </ng-container>
      </bms-dialog>
    }
  `,
})
export class AnnouncementsPage {
  private api = inject(AdminApi);
  private i18n = inject(TranslationService);
  private confirm = inject(ConfirmService);
  private toasts = inject(ToastService);
  private feed = inject(AnnouncementsFeed);

  protected readonly kinds: AnnouncementKind[] = ['INFO', 'WARNING'];
  protected readonly error = signal<string | null>(null);
  protected readonly editing = signal(false);

  protected editingId: string | null = null;
  protected form = blank();

  protected readonly announcements = rxResource({ stream: () => this.api.announcements() });

  protected kindLabel(kind: AnnouncementKind): MessageKey {
    return `announcements.kind.${kind}`;
  }

  protected phaseOf(announcement: Announcement): 'scheduled' | 'showing' | 'ended' {
    return phase(announcement, Date.now());
  }

  protected phaseLabel(announcement: Announcement): MessageKey {
    return `announcements.phase.${this.phaseOf(announcement)}`;
  }

  protected complete(): boolean {
    return (
      this.form.messageEn.trim() !== '' && this.form.startsAt !== '' && this.form.endsAt !== ''
    );
  }

  /** A blank form for a new one, or the announcement's own values to edit it. */
  protected open(announcement: Announcement | null): void {
    this.editingId = announcement?.id ?? null;
    this.form = announcement
      ? {
          kind: announcement.kind,
          messageEn: announcement.messageEn,
          messageFr: announcement.messageFr ?? '',
          messageDe: announcement.messageDe ?? '',
          startsAt: toLocalInput(announcement.startsAt),
          endsAt: toLocalInput(announcement.endsAt),
        }
      : blank();
    this.error.set(null);
    this.editing.set(true);
  }

  protected save(): void {
    const body = {
      ...this.form,
      startsAt: fromLocalInput(this.form.startsAt),
      endsAt: fromLocalInput(this.form.endsAt),
    };
    const request = this.editingId
      ? this.api.updateAnnouncement(this.editingId, body)
      : this.api.createAnnouncement(body);
    request.subscribe({
      next: () => this.changed(this.editingId ? 'announcements.saved' : 'announcements.published'),
      error: this.failed,
    });
  }

  protected async remove(announcement: Announcement): Promise<void> {
    const confirmed = await this.confirm.ask({
      heading: this.i18n.translate('announcements.deleteTitle'),
      body: this.i18n.translate('confirm.deleteBody'),
      action: this.i18n.translate('common.delete'),
    });
    if (!confirmed) {
      return;
    }
    this.api.removeAnnouncement(announcement.id).subscribe({
      next: () => this.changed('announcements.deleted'),
      error: this.failed,
    });
  }

  /** Every change shows at once, here and on the administrator's own banner. */
  private changed(key: MessageKey): void {
    this.editing.set(false);
    this.error.set(null);
    this.toasts.show(this.i18n.translate(key));
    this.announcements.reload();
    this.feed.reload();
  }

  private readonly failed = (response: HttpErrorResponse): void =>
    this.error.set(response.error?.detail ?? this.i18n.translate('announcements.saveFailed'));
}
