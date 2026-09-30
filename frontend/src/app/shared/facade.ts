import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { TranslationService } from '../core/i18n';
import { ApartmentStatus } from '../core/models';

export interface Unit {
  floor: number | null;
  status: ApartmentStatus;
}

/**
 * Stacks units into floors, top floor first, as a building is drawn. A unit
 * without a floor is taken to be on the ground floor.
 */
export function floorsOf(units: Unit[]): ApartmentStatus[][] {
  const byFloor = new Map<number, ApartmentStatus[]>();
  for (const unit of units) {
    const floor = unit.floor ?? 0;
    byFloor.set(floor, [...(byFloor.get(floor) ?? []), unit.status]);
  }
  return [...byFloor.entries()].sort(([a], [b]) => b - a).map(([, statuses]) => statuses);
}

const WINDOW_W = 10;
const WINDOW_H = 13;
const GAP = 6;
const SIDE = 10;
const ROOF = 12;
const GROUND = 22;

interface Pane {
  x: number;
  y: number;
  status: ApartmentStatus;
  /** Floors are counted from the ground, so the lights come on bottom up. */
  level: number;
}

/**
 * A building drawn as its floors, one window per apartment: lit when let, dark
 * when vacant, struck through while under maintenance. The drawing is the
 * occupancy, not a decoration of it.
 */
@Component({
  selector: 'bms-facade',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'facade', '[class.switching-on]': 'animate()' },
  template: `
    <svg
      [attr.width]="width() * scale()"
      [attr.height]="height() * scale()"
      [attr.viewBox]="'0 0 ' + width() + ' ' + height()"
      role="img"
      [attr.aria-label]="label()"
    >
      <rect class="body" x="1" [attr.y]="ROOF - 4" [attr.width]="width() - 2" [attr.height]="height() - ROOF + 3" rx="1.5" />
      <rect class="cornice" x="0" [attr.y]="ROOF - 6" [attr.width]="width()" height="3" rx="1" />
      @for (pane of panes(); track $index) {
        <rect
          class="pane {{ pane.status.toLowerCase() }}"
          [attr.x]="pane.x"
          [attr.y]="pane.y"
          [attr.width]="WINDOW_W"
          [attr.height]="WINDOW_H"
          rx="1"
          [style.animation-delay.ms]="pane.level * 110"
        />
        @if (pane.status === 'MAINTENANCE') {
          <line
            class="strike"
            [attr.x1]="pane.x + 2"
            [attr.y1]="pane.y + WINDOW_H - 2"
            [attr.x2]="pane.x + WINDOW_W - 2"
            [attr.y2]="pane.y + 2"
          />
        }
      }
      <rect class="door" [attr.x]="width() / 2 - 6" [attr.y]="height() - 17" width="12" height="16" rx="1.5" />
    </svg>
  `,
})
export class Facade {
  readonly units = input.required<Unit[]>();
  readonly scale = input(1);
  readonly animate = input(false);

  private readonly i18n = inject(TranslationService);

  protected readonly label = computed(() =>
    this.i18n.translate('facade.label', {
      occupied: this.units().filter((unit) => unit.status === 'OCCUPIED').length,
      total: this.units().length,
    }),
  );

  protected readonly ROOF = ROOF;
  protected readonly WINDOW_W = WINDOW_W;
  protected readonly WINDOW_H = WINDOW_H;

  private readonly floors = computed(() => floorsOf(this.units()));
  private readonly columns = computed(() =>
    Math.max(2, ...this.floors().map((floor) => floor.length)),
  );

  protected readonly width = computed(
    () => SIDE * 2 + this.columns() * WINDOW_W + (this.columns() - 1) * GAP,
  );
  protected readonly height = computed(
    () => ROOF + Math.max(1, this.floors().length) * (WINDOW_H + GAP) + GROUND - GAP,
  );

  protected readonly panes = computed<Pane[]>(() => {
    const floors = this.floors();
    return floors.flatMap((statuses, row) =>
      statuses.map((status, column) => ({
        x: SIDE + column * (WINDOW_W + GAP),
        y: ROOF + row * (WINDOW_H + GAP),
        status,
        level: floors.length - 1 - row,
      })),
    );
  });
}
