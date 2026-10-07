import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  Injectable,
  computed,
  inject,
  signal,
} from '@angular/core';
import { EMPTY, Observable, Subscription, catchError, switchMap, timer } from 'rxjs';

import { AnnouncementsApi } from '../core/api';
import { Language, TranslationService } from '../core/i18n';
import { Announcement } from '../core/models';
import { Icon } from './icon';
import { IconButton } from './icon-button';
import { TranslatePipe } from './translate.pipe';

/** Often enough that a maintenance notice is seen before the maintenance. */
const REFRESH_MS = 5 * 60 * 1000;

const STORAGE_KEY = 'bms.dismissedAnnouncements';

/** The reader's language, or English when the administrator wrote none in it. */
export function announcementText(announcement: Announcement, language: Language): string {
  const translated = {
    en: announcement.messageEn,
    fr: announcement.messageFr,
    de: announcement.messageDe,
  };
  return translated[language] || announcement.messageEn;
}

/** Names one version of an announcement: an edit makes a new one, which shows again. */
export function dismissalKey(announcement: Announcement): string {
  return `${announcement.id}@${announcement.updatedAt}`;
}

/**
 * What the administrator is telling everybody right now, minus what this
 * browser has put away. It asks again every few minutes while the signed in
 * shell is up, and at once whenever the administrator's own screen changes one.
 */
@Injectable({ providedIn: 'root' })
export class AnnouncementsFeed {
  private api = inject(AnnouncementsApi);
  private polling: Subscription | null = null;

  private readonly showing = signal<Announcement[]>([]);
  private readonly dismissed = signal<string[]>(
    JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]'),
  );

  readonly visible = computed(() =>
    this.showing().filter((announcement) => !this.dismissed().includes(dismissalKey(announcement))),
  );

  start(): void {
    this.polling ??= timer(0, REFRESH_MS)
      .pipe(switchMap(() => this.fetch()))
      .subscribe((announcements) => this.showing.set(announcements));
  }

  /** Signed out: nobody is left to tell, and the next person starts from nothing. */
  stop(): void {
    this.polling?.unsubscribe();
    this.polling = null;
    this.showing.set([]);
  }

  reload(): void {
    this.fetch().subscribe((announcements) => this.showing.set(announcements));
  }

  /** Remembers only what is still showing, so the list never outgrows the announcements themselves. */
  dismiss(announcement: Announcement): void {
    const showingKeys = this.showing().map(dismissalKey);
    const keys = [
      ...this.dismissed().filter((key) => showingKeys.includes(key)),
      dismissalKey(announcement),
    ];
    this.dismissed.set(keys);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(keys));
  }

  /** A failed request keeps what is already shown, and the next one tries again. */
  private fetch(): Observable<Announcement[]> {
    return this.api.showing().pipe(catchError(() => EMPTY));
  }
}

/** The announcements above every screen; it keeps the feed running for as long as it is shown. */
@Component({
  selector: 'bms-announcements',
  imports: [Icon, IconButton, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @for (announcement of feed.visible(); track announcement.id) {
      <div class="notice announcement {{ announcement.kind.toLowerCase() }}" role="status">
        <bms-icon [name]="announcement.kind === 'WARNING' ? 'alert' : 'info'" />
        <p>{{ text(announcement) }}</p>
        <button
          bmsIconButton
          icon="close"
          [label]="'announcements.dismiss' | t"
          (click)="feed.dismiss(announcement)"
        ></button>
      </div>
    }
  `,
})
export class Announcements {
  private i18n = inject(TranslationService);
  protected readonly feed = inject(AnnouncementsFeed);

  constructor() {
    this.feed.start();
    inject(DestroyRef).onDestroy(() => this.feed.stop());
  }

  protected text(announcement: Announcement): string {
    return announcementText(announcement, this.i18n.language());
  }
}
