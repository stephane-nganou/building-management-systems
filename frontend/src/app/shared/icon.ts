import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * The handful of line icons the app uses, drawn on a 24 unit grid with a round
 * 1.75 stroke. Inline, so they inherit the text colour and cost no request.
 */
const PATHS = {
  dashboard: 'M3 3h7v9H3z M14 3h7v5h-7z M14 12h7v9h-7z M3 16h7v5H3z',
  building: 'M4 21V3h12v18 M16 9h4v12 M2 21h20 M8 7h1 M11 7h1 M8 11h1 M11 11h1 M8 15h1 M11 15h1',
  door: 'M5 21V4a1 1 0 0 1 1-1h12a1 1 0 0 1 1 1v17 M3 21h18 M15 12h.01',
  users:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M22 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75',
  receipt: 'M5 2v20l2.5-1.5L10 22l2-1.5 2 1.5 2.5-1.5L19 22V2l-2.5 1.5L14 2l-2 1.5L10 2 7.5 3.5z M9 8h6 M9 12h6 M9 16h3',
  invoice: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z M14 2v6h6 M8 13h8 M8 17h5',
  chart: 'M3 3v18h18 M8 17v-5 M13 17V8 M18 17v-9',
  assistant:
    'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8 M16 11l2 2 4-4',
  shield: 'M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4',
  calendar: 'M4 5h16v16H4z M4 10h16 M8 3v4 M16 3v4',
  pause: 'M8 5v14 M16 5v14',
  play: 'M7 4l13 8-13 8z',
  plus: 'M12 5v14 M5 12h14',
  edit: 'M12 20h9 M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z',
  trash: 'M3 6h18 M8 6V4h8v2 M19 6l-1 14H6L5 6 M10 11v6 M14 11v6',
  download: 'M12 3v12 M7 10l5 5 5-5 M5 21h14',
  send: 'M22 2 11 13 M22 2l-7 20-4-9-9-4z',
  paid: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z M8 12l3 3 5-6',
  print: 'M6 9V2h12v7 M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2 M6 14h12v8H6z',
  key: 'M15.5 7.5a3.5 3.5 0 1 1-2.9 5.46L4 21.5V18h2v-2h2v-2h1.46A3.5 3.5 0 0 1 15.5 7.5z M16.5 9.5h.01',
  copy: 'M8 8h12v12H8z M16 8V4H4v12h4',
  check: 'M20 6 9 17l-5-5',
  close: 'M18 6 6 18 M6 6l12 12',
  menu: 'M4 6h16 M4 12h16 M4 18h16',
  signOut: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4 M16 17l5-5-5-5 M21 12H9',
  alert: 'M12 9v4 M12 17h.01 M10.3 3.9 1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z',
  info: 'M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0z M12 16v-4 M12 8h.01',
  megaphone: 'M3 11l18-5v12L3 14z M11.6 16.8a3 3 0 1 1-5.8-1.6',
  mail: 'M3 5h18v14H3z M3 7l9 6 9-6',
  phone:
    'M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z',
  pin: 'M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z M12 13a3 3 0 1 0 0-6 3 3 0 0 0 0 6',
} as const;

export type IconName = keyof typeof PATHS;

@Component({
  selector: 'bms-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      <path [attr.d]="path()" />
    </svg>
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(18);

  protected readonly path = computed(() => PATHS[this.name()]);
}
